/**
 * Copyright (C) 2009 Scalable Solutions.
 */

package se.scalablesolutions.akka.dispatch

import java.util.concurrent._
import locks.ReentrantLock
import atomic.{AtomicLong, AtomicInteger}
import ThreadPoolExecutor.CallerRunsPolicy

import java.util.{Collection, HashSet, HashMap, LinkedList, List}

/**
 * Implements the Reactor pattern as defined in: [http://www.cs.wustl.edu/~schmidt/PDF/reactor-siemens.pdf].<br/>
 * See also this article: [http://today.java.net/cs/user/print/a/350].
 * <p/>
 * 
 * Default settings are:
 * <pre/>
 *   - withNewThreadPoolWithLinkedBlockingQueueWithUnboundedCapacity
 *   - NR_START_THREADS = 16
 *   - NR_MAX_THREADS = 128
 *   - KEEP_ALIVE_TIME = 60000L // one minute
 * </pre>
 * <p/>
 * 
 * The dispatcher has a fluent builder interface to build up a thread pool to suite your use-case. 
 * There is a default thread pool defined but make use of the builder if you need it. Here are some examples.
 * <p/>
 * 
 * Scala API.
 * <p/>
 * Example usage:
 * <pre/>
 *   val dispatcher = new EventBasedThreadPoolDispatcher("name", false)
 *   dispatcher
 *     .withNewThreadPoolWithBoundedBlockingQueue(100)
 *     .setCorePoolSize(16)
 *     .setMaxPoolSize(128)
 *     .setKeepAliveTimeInMillis(60000)
 *     .setRejectionPolicy(new CallerRunsPolicy)
 *     .buildThreadPool
 * </pre>
 * <p/>
 * 
 * Java API.
 * <p/>
 * Example usage:
 * <pre/>
 *   EventBasedThreadPoolDispatcher dispatcher = new EventBasedThreadPoolDispatcher("name", false);
 *   dispatcher
 *     .withNewThreadPoolWithBoundedBlockingQueue(100)
 *     .setCorePoolSize(16)
 *     .setMaxPoolSize(128)
 *     .setKeepAliveTimeInMillis(60000)
 *     .setRejectionPolicy(new CallerRunsPolicy())
 *     .buildThreadPool();
 * </pre>
 * <p/>
 *
 * But the preferred way of creating dispatchers is to use 
 * the {@link se.scalablesolutions.akka.dispatch.Dispatchers} factory object.
 * 
 * @author <a href="http://jonasboner.com">Jonas Bon&#233;r</a>
 */
class EventBasedThreadPoolDispatcher(name: String, private val concurrentMode: Boolean) extends MessageDispatcherBase(name) {
  def this(name: String) = this(name, false)
  
  private val NR_START_THREADS = 16
  private val NR_MAX_THREADS = 128
  private val KEEP_ALIVE_TIME = 60000L // default is one minute
  private var inProcessOfBuilding = false
  private var executor: ExecutorService = _
  private var threadPoolBuilder: ThreadPoolExecutor = _
  private val threadFactory = new MonitorableThreadFactory("akka:" + name)
  private var boundedExecutorBound = -1
  private val busyInvokers = new HashSet[AnyRef]

  // build default thread pool
  withNewThreadPoolWithLinkedBlockingQueueWithUnboundedCapacity.buildThreadPool
  
  def start = if (!active) {
    active = true

    /**
     * This dispatcher code is based on code from the actorom actor framework by Sergio Bossa [http://code.google.com/p/actorom/].
     */
    val messageDemultiplexer = new EventBasedThreadPoolDemultiplexer(queue)
    selectorThread = new Thread {
      override def run = {
        while (active) {
          try {
            try {
              guard.synchronized { /* empty */ } // prevents risk for deadlock as described in [http://developers.sun.com/learning/javaoneonline/2006/coreplatform/TS-1315.pdf]
              messageDemultiplexer.select
            } catch { case e: InterruptedException => active = false }
            val selectedInvocations = messageDemultiplexer.acquireSelectedInvocations
            val reservedInvocations = reserve(selectedInvocations)
            val it = reservedInvocations.entrySet.iterator
            while (it.hasNext) {
              val entry = it.next
              val invocation = entry.getKey
              val invoker = entry.getValue
              threadPoolBuilder.execute(new Runnable() {
                def run = {
                  invoker.invoke(invocation)
                  free(invocation.receiver)
                  messageDemultiplexer.wakeUp
                }
              })
            }
          } finally {
            messageDemultiplexer.releaseSelectedInvocations
          }
        }
      }
    };
    selectorThread.start
  }

  override protected def doShutdown = executor.shutdownNow

  private def reserve(invocations: List[MessageInvocation]): HashMap[MessageInvocation, MessageInvoker] = guard.synchronized {
    val result = new HashMap[MessageInvocation, MessageInvoker]
    val iterator = invocations.iterator
    while (iterator.hasNext) {
      val invocation = iterator.next
      if (invocation == null) throw new IllegalStateException("Message invocation is null [" + invocation + "]")
      if (concurrentMode) {
        val invoker = messageHandlers.get(invocation.receiver)
        if (invoker == null) throw new IllegalStateException("Message invoker for invocation [" + invocation + "] is null")
        result.put(invocation, invoker)
      } else if (!busyInvokers.contains(invocation.receiver)) {
        val invoker = messageHandlers.get(invocation.receiver)
        if (invoker == null) throw new IllegalStateException("Message invoker for invocation [" + invocation + "] is null")
        result.put(invocation, invoker)
        busyInvokers.add(invocation.receiver)
        iterator.remove
      }
    }
    result
  }

  private def free(invoker: AnyRef) = guard.synchronized {
    if (!concurrentMode) busyInvokers.remove(invoker)
  }
  
  // ============ Code for configuration of thread pool =============

  def buildThreadPool = synchronized {
    ensureNotActive
    inProcessOfBuilding = false
    if (boundedExecutorBound > 0) {
      val boundedExecutor = new BoundedExecutorDecorator(threadPoolBuilder, boundedExecutorBound)
      boundedExecutorBound = -1
      executor = boundedExecutor
    } else {
      executor = threadPoolBuilder
    }
  }

  def withNewThreadPoolWithQueue(queue: BlockingQueue[Runnable]): EventBasedThreadPoolDispatcher = synchronized {
    ensureNotActive
    verifyNotInConstructionPhase
    inProcessOfBuilding = false
    blockingQueue = queue
    threadPoolBuilder = new ThreadPoolExecutor(NR_START_THREADS, NR_MAX_THREADS, KEEP_ALIVE_TIME, MILLISECONDS, queue)
    this
  }

  /**
   * Creates a new thread pool in which the number of tasks in the pending queue is bounded. Will block when exceeeded.
   * <p/>
   * The 'bound' variable should specify the number equal to the size of the thread pool PLUS the number of queued tasks that should be followed.
   */
  def withNewThreadPoolWithBoundedBlockingQueue(bound: Int): EventBasedThreadPoolDispatcher = synchronized {
    ensureNotActive
    verifyNotInConstructionPhase
    blockingQueue = new LinkedBlockingQueue[Runnable]
    threadPoolBuilder = new ThreadPoolExecutor(NR_START_THREADS, NR_MAX_THREADS, KEEP_ALIVE_TIME, MILLISECONDS, blockingQueue, threadFactory)
    boundedExecutorBound = bound
    this
  }

  def withNewThreadPoolWithLinkedBlockingQueueWithCapacity(capacity: Int): EventBasedThreadPoolDispatcher = synchronized {
    ensureNotActive
    verifyNotInConstructionPhase
    blockingQueue = new LinkedBlockingQueue[Runnable](capacity) 
    threadPoolBuilder = new ThreadPoolExecutor(NR_START_THREADS, NR_MAX_THREADS, KEEP_ALIVE_TIME, MILLISECONDS, blockingQueue, threadFactory, new CallerRunsPolicy)
    this
  }

  def withNewThreadPoolWithLinkedBlockingQueueWithUnboundedCapacity: EventBasedThreadPoolDispatcher = synchronized {
    ensureNotActive
    verifyNotInConstructionPhase
    blockingQueue = new LinkedBlockingQueue[Runnable]
    threadPoolBuilder = new ThreadPoolExecutor(NR_START_THREADS, NR_MAX_THREADS, KEEP_ALIVE_TIME, MILLISECONDS, blockingQueue, threadFactory, new CallerRunsPolicy)
    this
  }

  def withNewThreadPoolWithSynchronousQueueWithFairness(fair: Boolean): EventBasedThreadPoolDispatcher = synchronized {
    ensureNotActive
    verifyNotInConstructionPhase
    blockingQueue = new SynchronousQueue[Runnable](fair)
    threadPoolBuilder = new ThreadPoolExecutor(NR_START_THREADS, NR_MAX_THREADS, KEEP_ALIVE_TIME, MILLISECONDS, blockingQueue, threadFactory, new CallerRunsPolicy)
    this
  }

  def withNewThreadPoolWithArrayBlockingQueueWithCapacityAndFairness(capacity: Int, fair: Boolean): EventBasedThreadPoolDispatcher = synchronized {
    ensureNotActive
    verifyNotInConstructionPhase
    blockingQueue = new ArrayBlockingQueue[Runnable](capacity, fair)
    threadPoolBuilder = new ThreadPoolExecutor(NR_START_THREADS, NR_MAX_THREADS, KEEP_ALIVE_TIME, MILLISECONDS, blockingQueue, threadFactory, new CallerRunsPolicy)
    this
  }

  /**
   * Default is 16.
   */
  def setCorePoolSize(size: Int): EventBasedThreadPoolDispatcher = synchronized {
    ensureNotActive
    verifyInConstructionPhase
    threadPoolBuilder.setCorePoolSize(size)
    this
  }

  /**
   * Default is 128.
   */
  def setMaxPoolSize(size: Int): EventBasedThreadPoolDispatcher = synchronized {
    ensureNotActive
    verifyInConstructionPhase
    threadPoolBuilder.setMaximumPoolSize(size)
    this
  }

  /**
   * Default is 60000 (one minute).
   */
  def setKeepAliveTimeInMillis(time: Long): EventBasedThreadPoolDispatcher = synchronized {
    ensureNotActive
    verifyInConstructionPhase
    threadPoolBuilder.setKeepAliveTime(time, MILLISECONDS)
    this
  }

  /**
   * Default ThreadPoolExecutor.CallerRunsPolicy. To allow graceful backing off when pool is overloaded.
   */
  def setRejectionPolicy(policy: RejectedExecutionHandler): EventBasedThreadPoolDispatcher = synchronized {
    ensureNotActive
    verifyInConstructionPhase
    threadPoolBuilder.setRejectedExecutionHandler(policy)
    this
  }

  private def verifyNotInConstructionPhase = {
    if (inProcessOfBuilding) throw new IllegalStateException("Is already in the process of building a thread pool")
    inProcessOfBuilding = true
  }

  private def verifyInConstructionPhase = {
    if (!inProcessOfBuilding) throw new IllegalStateException("Is not in the process of building a thread pool, start building one by invoking one of the 'newThreadPool*' methods")
  }

  private def ensureNotActive = if (active) throw new IllegalStateException("Can't build a new thread pool for a dispatcher that is already up and running")  
}

class EventBasedThreadPoolDemultiplexer(private val messageQueue: ReactiveMessageQueue) extends MessageDemultiplexer {
  private val selectedInvocations: List[MessageInvocation] = new LinkedList[MessageInvocation]
  private val selectedInvocationsLock = new ReentrantLock

  def select = try {
    selectedInvocationsLock.lock
    messageQueue.read(selectedInvocations)
  } finally {
    selectedInvocationsLock.unlock
  }

  def acquireSelectedInvocations: List[MessageInvocation] = {
    selectedInvocationsLock.lock
    selectedInvocations
  }

  def releaseSelectedInvocations = selectedInvocationsLock.unlock

  def wakeUp = messageQueue.interrupt
}

/**
 * @author <a href="http://jonasboner.com">Jonas Bon&#233;r</a>
 */
class BoundedExecutorDecorator(val executor: ExecutorService, bound: Int) extends ExecutorService {
  private val semaphore = new Semaphore(bound)

  def execute(command: Runnable) = {
    semaphore.acquire
    try {
      executor.execute(new Runnable() {
        def run = {
          try {
            command.run
          } finally {
            semaphore.release
          }
        }
      })
    } catch {
      case e: RejectedExecutionException =>
        semaphore.release
    }
  }

  // Delegating methods for the ExecutorService interface
  def shutdown = executor.shutdown
  def shutdownNow = executor.shutdownNow
  def isShutdown = executor.isShutdown
  def isTerminated = executor.isTerminated
  def awaitTermination(l: Long, timeUnit: TimeUnit) = executor.awaitTermination(l, timeUnit)
  def submit[T](callable: Callable[T]) = executor.submit(callable)
  def submit[T](runnable: Runnable, t: T) = executor.submit(runnable, t)
  def submit(runnable: Runnable) = executor.submit(runnable)
  def invokeAll[T](callables: Collection[_ <: Callable[T]]) = executor.invokeAll(callables)
  def invokeAll[T](callables: Collection[_ <: Callable[T]], l: Long, timeUnit: TimeUnit) = executor.invokeAll(callables, l, timeUnit)
  def invokeAny[T](callables: Collection[_ <: Callable[T]]) = executor.invokeAny(callables)
  def invokeAny[T](callables: Collection[_ <: Callable[T]], l: Long, timeUnit: TimeUnit) = executor.invokeAny(callables, l, timeUnit)
}

/**
 * @author <a href="http://jonasboner.com">Jonas Bon&#233;r</a>
 */
class MonitorableThreadFactory(val name: String) extends ThreadFactory {
  private val counter = new AtomicLong
  def newThread(runnable: Runnable) =
    //new MonitorableThread(runnable, name)
    new Thread(runnable, name + "-" + counter.getAndIncrement)
}

/**
 * @author <a href="http://jonasboner.com">Jonas Bon&#233;r</a>
 */
object MonitorableThread {
  val DEFAULT_NAME = "MonitorableThread"
  val created = new AtomicInteger
  val alive = new AtomicInteger
  @volatile val debugLifecycle = false
}

// FIXME fix the issues with using the monitoring in MonitorableThread
/**
 * @author <a href="http://jonasboner.com">Jonas Bon&#233;r</a>
 */
class MonitorableThread(runnable: Runnable, name: String)
  extends Thread(runnable, name + "-" + MonitorableThread.created.incrementAndGet) {//with Logging {
  setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
    def uncaughtException(thread: Thread, cause: Throwable) = {} //log.error("UNCAUGHT in thread [%s] cause [%s]", thread.getName, cause)
  })

  override def run = {
    val debug = MonitorableThread.debugLifecycle
    //if (debug) log.debug("Created %s", getName)
    try {
       MonitorableThread.alive.incrementAndGet
       super.run
     } finally {
        MonitorableThread.alive.decrementAndGet
        //if (debug) log.debug("Exiting %s", getName)
      }
   }
}

