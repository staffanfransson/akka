/**
 * Copyright (C) 2009 Scalable Solutions.
 */

package se.scalablesolutions.akka.actor

import java.net.InetSocketAddress

import se.scalablesolutions.akka.dispatch.{MessageDispatcher, FutureResult}
import se.scalablesolutions.akka.nio.protobuf.RemoteProtocol.RemoteRequest
import se.scalablesolutions.akka.nio.{RemoteProtocolBuilder, RemoteClient, RemoteRequestIdFactory}
import se.scalablesolutions.akka.config.ScalaConfig._
import se.scalablesolutions.akka.serialization.Serializer
import se.scalablesolutions.akka.util._

import org.codehaus.aspectwerkz.joinpoint.{MethodRtti, JoinPoint}
import org.codehaus.aspectwerkz.proxy.Proxy
import org.codehaus.aspectwerkz.annotation.{Aspect, Around}

import java.lang.reflect.{InvocationTargetException, Method}

object Annotations {
  import se.scalablesolutions.akka.annotation._
  val oneway =                 classOf[oneway]
  val transactionrequired =    classOf[transactionrequired]
  val prerestart =             classOf[prerestart]
  val postrestart =            classOf[postrestart]
  val immutable =              classOf[immutable]
  val inittransactionalstate = classOf[inittransactionalstate]
}

/**
 *
 * @author <a href="http://jonasboner.com">Jonas Bon&#233;r</a>
 */
object ActiveObject {
  val AKKA_CAMEL_ROUTING_SCHEME = "akka"

  def newInstance[T](target: Class[T], timeout: Long): T =
    newInstance(target, new Dispatcher(false, None), None, timeout)

  def newInstance[T](target: Class[T], timeout: Long, restartCallbacks: Option[RestartCallbacks]): T =
    newInstance(target, new Dispatcher(false, restartCallbacks), None, timeout)

  def newInstance[T](intf: Class[T], target: AnyRef, timeout: Long): T =
    newInstance(intf, target, new Dispatcher(false, None), None, timeout)

  def newInstance[T](intf: Class[T], target: AnyRef, timeout: Long, restartCallbacks: Option[RestartCallbacks]): T =
    newInstance(intf, target, new Dispatcher(false, restartCallbacks), None, timeout)

  def newInstance[T](target: Class[T], timeout: Long, transactionRequired: Boolean): T =
    newInstance(target, new Dispatcher(transactionRequired, None), None, timeout)

  def newInstance[T](target: Class[T], timeout: Long, transactionRequired: Boolean, restartCallbacks: Option[RestartCallbacks]): T =
    newInstance(target, new Dispatcher(transactionRequired, restartCallbacks), None, timeout)

  def newInstance[T](intf: Class[T], target: AnyRef, timeout: Long, transactionRequired: Boolean): T =
    newInstance(intf, target, new Dispatcher(transactionRequired, None), None, timeout)

  def newInstance[T](intf: Class[T], target: AnyRef, timeout: Long, transactionRequired: Boolean, restartCallbacks: Option[RestartCallbacks]): T =
    newInstance(intf, target, new Dispatcher(transactionRequired, restartCallbacks), None, timeout)

  def newRemoteInstance[T](target: Class[T], timeout: Long, hostname: String, port: Int): T =
    newInstance(target, new Dispatcher(false, None), Some(new InetSocketAddress(hostname, port)), timeout)

  def newRemoteInstance[T](target: Class[T], timeout: Long, hostname: String, port: Int, restartCallbacks: Option[RestartCallbacks]): T =
    newInstance(target, new Dispatcher(false, restartCallbacks), Some(new InetSocketAddress(hostname, port)), timeout)

  def newRemoteInstance[T](intf: Class[T], target: AnyRef, timeout: Long, hostname: String, port: Int): T =
    newInstance(intf, target, new Dispatcher(false, None), Some(new InetSocketAddress(hostname, port)), timeout)

  def newRemoteInstance[T](intf: Class[T], target: AnyRef, timeout: Long, hostname: String, port: Int, restartCallbacks: Option[RestartCallbacks]): T =
    newInstance(intf, target, new Dispatcher(false, restartCallbacks), Some(new InetSocketAddress(hostname, port)), timeout)

  def newRemoteInstance[T](target: Class[T], timeout: Long, transactionRequired: Boolean, hostname: String, port: Int): T =
    newInstance(target, new Dispatcher(transactionRequired, None), Some(new InetSocketAddress(hostname, port)), timeout)

  def newRemoteInstance[T](target: Class[T], timeout: Long, transactionRequired: Boolean, hostname: String, port: Int, restartCallbacks: Option[RestartCallbacks]): T =
    newInstance(target, new Dispatcher(transactionRequired, restartCallbacks), Some(new InetSocketAddress(hostname, port)), timeout)

  def newRemoteInstance[T](intf: Class[T], target: AnyRef, timeout: Long, transactionRequired: Boolean, hostname: String, port: Int): T =
    newInstance(intf, target, new Dispatcher(transactionRequired, None), Some(new InetSocketAddress(hostname, port)), timeout)

  def newRemoteInstance[T](intf: Class[T], target: AnyRef, timeout: Long, transactionRequired: Boolean, hostname: String, port: Int, restartCallbacks: Option[RestartCallbacks]): T =
    newInstance(intf, target, new Dispatcher(transactionRequired, restartCallbacks), Some(new InetSocketAddress(hostname, port)), timeout)

  def newInstance[T](target: Class[T], timeout: Long, dispatcher: MessageDispatcher): T = {
    val actor = new Dispatcher(false, None)
    actor.messageDispatcher = dispatcher
    newInstance(target, actor, None, timeout)
  }

  def newInstance[T](target: Class[T], timeout: Long, dispatcher: MessageDispatcher, restartCallbacks: Option[RestartCallbacks]): T = {
    val actor = new Dispatcher(false, restartCallbacks)
    actor.messageDispatcher = dispatcher
    newInstance(target, actor, None, timeout)
  }

  def newInstance[T](intf: Class[T], target: AnyRef, timeout: Long, dispatcher: MessageDispatcher): T = {
    val actor = new Dispatcher(false, None)
    actor.messageDispatcher = dispatcher
    newInstance(intf, target, actor, None, timeout)
  }

  def newInstance[T](intf: Class[T], target: AnyRef, timeout: Long, dispatcher: MessageDispatcher, restartCallbacks: Option[RestartCallbacks]): T = {
    val actor = new Dispatcher(false, restartCallbacks)
    actor.messageDispatcher = dispatcher
    newInstance(intf, target, actor, None, timeout)
  }

  def newInstance[T](target: Class[T], timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher): T = {
    val actor = new Dispatcher(transactionRequired, None)
    actor.messageDispatcher = dispatcher
    newInstance(target, actor, None, timeout)
  }

  def newInstance[T](target: Class[T], timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher, restartCallbacks: Option[RestartCallbacks]): T = {
    val actor = new Dispatcher(transactionRequired, restartCallbacks)
    actor.messageDispatcher = dispatcher
    newInstance(target, actor, None, timeout)
  }

  def newInstance[T](intf: Class[T], target: AnyRef, timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher): T = {
    val actor = new Dispatcher(transactionRequired, None)
    actor.messageDispatcher = dispatcher
    newInstance(intf, target, actor, None, timeout)
  }

  def newInstance[T](intf: Class[T], target: AnyRef, timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher, restartCallbacks: Option[RestartCallbacks]): T = {
    val actor = new Dispatcher(transactionRequired, restartCallbacks)
    actor.messageDispatcher = dispatcher
    newInstance(intf, target, actor, None, timeout)
  }

  def newRemoteInstance[T](target: Class[T], timeout: Long, dispatcher: MessageDispatcher, hostname: String, port: Int): T = {
    val actor = new Dispatcher(false, None)
    actor.messageDispatcher = dispatcher
    newInstance(target, actor, Some(new InetSocketAddress(hostname, port)), timeout)
  }

  def newRemoteInstance[T](target: Class[T], timeout: Long, dispatcher: MessageDispatcher, hostname: String, port: Int, restartCallbacks: Option[RestartCallbacks]): T = {
    val actor = new Dispatcher(false, restartCallbacks)
    actor.messageDispatcher = dispatcher
    newInstance(target, actor, Some(new InetSocketAddress(hostname, port)), timeout)
  }

  def newRemoteInstance[T](intf: Class[T], target: AnyRef, timeout: Long, dispatcher: MessageDispatcher, hostname: String, port: Int): T = {
    val actor = new Dispatcher(false, None)
    actor.messageDispatcher = dispatcher
    newInstance(intf, target, actor, Some(new InetSocketAddress(hostname, port)), timeout)
  }

  def newRemoteInstance[T](intf: Class[T], target: AnyRef, timeout: Long, dispatcher: MessageDispatcher, hostname: String, port: Int, restartCallbacks: Option[RestartCallbacks]): T = {
    val actor = new Dispatcher(false, restartCallbacks)
    actor.messageDispatcher = dispatcher
    newInstance(intf, target, actor, Some(new InetSocketAddress(hostname, port)), timeout)
  }

  def newRemoteInstance[T](target: Class[T], timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher, hostname: String, port: Int): T = {
    val actor = new Dispatcher(transactionRequired, None)
    actor.messageDispatcher = dispatcher
    newInstance(target, actor, Some(new InetSocketAddress(hostname, port)), timeout)
  }

  def newRemoteInstance[T](target: Class[T], timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher, hostname: String, port: Int, restartCallbacks: Option[RestartCallbacks]): T = {
    val actor = new Dispatcher(transactionRequired, restartCallbacks)
    actor.messageDispatcher = dispatcher
    newInstance(target, actor, Some(new InetSocketAddress(hostname, port)), timeout)
  }

  def newRemoteInstance[T](intf: Class[T], target: AnyRef, timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher, hostname: String, port: Int): T = {
    val actor = new Dispatcher(transactionRequired, None)
    actor.messageDispatcher = dispatcher
    newInstance(intf, target, actor, Some(new InetSocketAddress(hostname, port)), timeout)
  }

  def newRemoteInstance[T](intf: Class[T], target: AnyRef, timeout: Long, transactionRequired: Boolean, dispatcher: MessageDispatcher, hostname: String, port: Int, restartCallbacks: Option[RestartCallbacks]): T = {
    val actor = new Dispatcher(transactionRequired, restartCallbacks)
    actor.messageDispatcher = dispatcher
    newInstance(intf, target, actor, Some(new InetSocketAddress(hostname, port)), timeout)
  }

  private[akka] def newInstance[T](target: Class[T], actor: Dispatcher, remoteAddress: Option[InetSocketAddress], timeout: Long): T = {
    val proxy = Proxy.newInstance(target, false, true)
    actor.initialize(target, proxy)
    actor.timeout = timeout
    actor.start
    AspectInitRegistry.register(proxy, AspectInit(target, actor, remoteAddress, timeout))
    proxy.asInstanceOf[T]
  }

  private[akka] def newInstance[T](intf: Class[T], target: AnyRef, actor: Dispatcher, remoteAddress: Option[InetSocketAddress], timeout: Long): T = {
    val proxy = Proxy.newInstance(Array(intf), Array(target), false, true)
    actor.initialize(target.getClass, target)
    actor.timeout = timeout
    actor.start
    AspectInitRegistry.register(proxy, AspectInit(intf, actor, remoteAddress, timeout))
    proxy.asInstanceOf[T]
  }


  private[akka] def supervise(restartStrategy: RestartStrategy, components: List[Supervise]): Supervisor = {
    val factory = SupervisorFactory(SupervisorConfig(restartStrategy, components))
    val supervisor = factory.newInstance
    supervisor.start
    supervisor
  }
}

private[akka] object AspectInitRegistry {
  private val inits = new java.util.concurrent.ConcurrentHashMap[AnyRef, AspectInit]

  def initFor(target: AnyRef) = {
    val init = inits.get(target)
    inits.remove(target)
    init
  }  

  def register(target: AnyRef, init: AspectInit) = inits.put(target, init)
}

private[akka] sealed case class AspectInit(
  val target: Class[_],
  val actor: Dispatcher,          
  val remoteAddress: Option[InetSocketAddress],
  val timeout: Long){

    def this(target: Class[_],actor: Dispatcher, timeout: Long) = this(target,actor,None,timeout)
  }
      
/**
 * AspectWerkz Aspect that is turning POJOs into Active Object.
 * Is deployed on a 'per-instance' basis.
 *
 * @author <a href="http://jonasboner.com">Jonas Bon&#233;r</a>
 */
@Aspect("perInstance")
private[akka] sealed class ActiveObjectAspect {
  
  @volatile var isInitialized = false
  var target: Class[_] = _
  var actor: Dispatcher = _            
  var remoteAddress: Option[InetSocketAddress] = _
  var timeout: Long = _

  @Around("execution(* *.*(..))")
  def invoke(joinPoint: JoinPoint): AnyRef = {
    if (!isInitialized) {
      val init = AspectInitRegistry.initFor(joinPoint.getThis)
      target = init.target
      actor = init.actor            
      remoteAddress = init.remoteAddress
      timeout = init.timeout
      isInitialized = true
    }
    dispatch(joinPoint)
  }

  private def dispatch(joinPoint: JoinPoint) = {
    if (remoteAddress.isDefined) remoteDispatch(joinPoint)
    else localDispatch(joinPoint)
  }

  private def localDispatch(joinPoint: JoinPoint): AnyRef = {
    import Actor.Sender.Self
    val rtti = joinPoint.getRtti.asInstanceOf[MethodRtti]
    if (isOneWay(rtti)) actor ! Invocation(joinPoint, true, true)
    else {
      val result = actor !! Invocation(joinPoint, false, isVoid(rtti))
      if (result.isDefined) result.get
      else throw new IllegalStateException("No result defined for invocation [" + joinPoint + "]")
    }
  }

  private def remoteDispatch(joinPoint: JoinPoint): AnyRef = {
    val rtti = joinPoint.getRtti.asInstanceOf[MethodRtti]
    val oneWay_? = isOneWay(rtti)
    val (message: Array[AnyRef], isEscaped) = escapeArguments(rtti.getParameterValues)
    val requestBuilder = RemoteRequest.newBuilder
      .setId(RemoteRequestIdFactory.nextId)
      .setMethod(rtti.getMethod.getName)
      .setTarget(target.getName)
      .setUuid(actor.uuid)
      .setTimeout(timeout)
      .setIsActor(false)
      .setIsOneWay(oneWay_?)
      .setIsEscaped(false)
    RemoteProtocolBuilder.setMessage(message, requestBuilder)
    val id = actor.registerSupervisorAsRemoteActor
    if (id.isDefined) requestBuilder.setSupervisorUuid(id.get)
    val remoteMessage = requestBuilder.build
    val future = RemoteClient.clientFor(remoteAddress.get).send(remoteMessage)
    if (oneWay_?) null // for void methods
    else {
      if (future.isDefined) {
        future.get.await
        val result = getResultOrThrowException(future.get)
        if (result.isDefined) result.get
        else throw new IllegalStateException("No result returned from call to [" + joinPoint + "]")
      } else throw new IllegalStateException("No future returned from call to [" + joinPoint + "]")
    }
  }

  private def getResultOrThrowException[T](future: FutureResult): Option[T] =
    if (future.exception.isDefined) {
      val (_, cause) = future.exception.get
      throw cause
    } else future.result.asInstanceOf[Option[T]]
  
  private def isOneWay(rtti: MethodRtti) = rtti.getMethod.isAnnotationPresent(Annotations.oneway)

  private def isVoid(rtti: MethodRtti) = rtti.getMethod.getReturnType == java.lang.Void.TYPE

  private def escapeArguments(args: Array[AnyRef]): Tuple2[Array[AnyRef], Boolean] = {
    var isEscaped = false
    val escapedArgs = for (arg <- args) yield {
      val clazz = arg.getClass
      if (clazz.getName.contains("$$ProxiedByAW")) {
        isEscaped = true
        "$$ProxiedByAW" + clazz.getSuperclass.getName
      } else arg
    }
    (escapedArgs, isEscaped)
  }
}

/**
 * Represents a snapshot of the current invocation.
 *
 * @author <a href="http://jonasboner.com">Jonas Bon&#233;r</a>
 */
@serializable private[akka] case class Invocation(joinPoint: JoinPoint, isOneWay: Boolean, isVoid: Boolean) {

  override def toString: String = synchronized {
    "Invocation [joinPoint: " + joinPoint.toString + ", isOneWay: " + isOneWay + ", isVoid: " + isVoid + "]"
  }

  override def hashCode: Int = synchronized {
    var result = HashCode.SEED
    result = HashCode.hash(result, joinPoint)
    result = HashCode.hash(result, isOneWay)
    result = HashCode.hash(result, isVoid)
    result
  }

  override def equals(that: Any): Boolean = synchronized {
    that != null &&
    that.isInstanceOf[Invocation] &&
    that.asInstanceOf[Invocation].joinPoint == joinPoint &&
    that.asInstanceOf[Invocation].isOneWay == isOneWay &&
    that.asInstanceOf[Invocation].isVoid == isVoid
  }
}

/**
 * Generic Actor managing Invocation dispatch, transaction and error management.
 *
 * @author <a href="http://jonasboner.com">Jonas Bon&#233;r</a>
 */
private[akka] class Dispatcher(transactionalRequired: Boolean, val callbacks: Option[RestartCallbacks]) extends Actor {
  private val ZERO_ITEM_CLASS_ARRAY = Array[Class[_]]()
  private val ZERO_ITEM_OBJECT_ARRAY = Array[Object[_]]()

  private[actor] var target: Option[AnyRef] = None
  private var preRestart: Option[Method] = None
  private var postRestart: Option[Method] = None
  private var initTxState: Option[Method] = None

  def this(transactionalRequired: Boolean) = this(transactionalRequired,None)

  private[actor] def initialize(targetClass: Class[_], targetInstance: AnyRef) = {
    if (transactionalRequired || targetClass.isAnnotationPresent(Annotations.transactionrequired)) makeTransactionRequired
    id = targetClass.getName
    target = Some(targetInstance)
    val methods = targetInstance.getClass.getDeclaredMethods.toList

    // See if we have any config define restart callbacks
    callbacks match {
      case None => {}
      case Some(RestartCallbacks(pre, post)) =>
        preRestart = Some(try {
          targetInstance.getClass.getDeclaredMethod(pre, ZERO_ITEM_CLASS_ARRAY: _*)
        } catch { case e => throw new IllegalStateException("Could not find pre restart method [" + pre + "] in [" + targetClass.getName + "]. It must have a zero argument definition.") })
        postRestart = Some(try {
          targetInstance.getClass.getDeclaredMethod(post, ZERO_ITEM_CLASS_ARRAY: _*)
        } catch { case e => throw new IllegalStateException("Could not find post restart method [" + post + "] in [" + targetClass.getName + "]. It must have a zero argument definition.") })
    }

    // See if we have any annotation defined restart callbacks 
    if (!preRestart.isDefined) preRestart = methods.find(m => m.isAnnotationPresent(Annotations.prerestart))
    if (!postRestart.isDefined) postRestart = methods.find(m => m.isAnnotationPresent(Annotations.postrestart))

    if (preRestart.isDefined && preRestart.get.getParameterTypes.length != 0)
      throw new IllegalStateException("Method annotated with @prerestart or defined as a restart callback in [" + targetClass.getName + "] must have a zero argument definition")
    if (postRestart.isDefined && postRestart.get.getParameterTypes.length != 0)
      throw new IllegalStateException("Method annotated with @postrestart or defined as a restart callback in [" + targetClass.getName + "] must have a zero argument definition")

    if (preRestart.isDefined) preRestart.get.setAccessible(true)
    if (postRestart.isDefined) postRestart.get.setAccessible(true)
    
    // see if we have a method annotated with @inittransactionalstate, if so invoke it
    //initTxState = methods.find(m => m.isAnnotationPresent(Annotations.inittransactionalstate))
    //if (initTxState.isDefined && initTxState.get.getParameterTypes.length != 0) throw new IllegalStateException("Method annotated with @inittransactionalstate must have a zero argument definition")
    //if (initTxState.isDefined) initTxState.get.setAccessible(true)
  }

  def receive: PartialFunction[Any, Unit] = {
    case Invocation(joinPoint, isOneWay, _) =>
      if (Actor.SERIALIZE_MESSAGES) serializeArguments(joinPoint)
      if (isOneWay) joinPoint.proceed
      else reply(joinPoint.proceed)
    case unexpected =>
      throw new IllegalStateException("Unexpected message [" + unexpected + "] sent to [" + this + "]")
  }

  override protected def preRestart(reason: AnyRef, config: Option[AnyRef]) {
    try {
      if (preRestart.isDefined) preRestart.get.invoke(target.get, ZERO_ITEM_OBJECT_ARRAY: _*)
    } catch { case e: InvocationTargetException => throw e.getCause }
  }

  override protected def postRestart(reason: AnyRef, config: Option[AnyRef]) {
    try {
      if (postRestart.isDefined) postRestart.get.invoke(target.get, ZERO_ITEM_OBJECT_ARRAY: _*)
    } catch { case e: InvocationTargetException => throw e.getCause }
  }

  //override protected def initTransactionalState = {
  //  try {
  //    if (initTxState.isDefined && target.isDefined) initTxState.get.invoke(target.get, ZERO_ITEM_OBJECT_ARRAY: _*)
  //  } catch { case e: InvocationTargetException => throw e.getCause }
  //}

  private def serializeArguments(joinPoint: JoinPoint) = {
    val args = joinPoint.getRtti.asInstanceOf[MethodRtti].getParameterValues
    var unserializable = false
    var hasMutableArgument = false
    for (arg <- args.toList) {
      if (!arg.isInstanceOf[String] &&
        !arg.isInstanceOf[Byte] &&
        !arg.isInstanceOf[Int] &&
        !arg.isInstanceOf[Long] &&
        !arg.isInstanceOf[Float] &&
        !arg.isInstanceOf[Double] &&
        !arg.isInstanceOf[Boolean] &&
        !arg.isInstanceOf[Char] &&
        !arg.isInstanceOf[java.lang.Byte] &&
        !arg.isInstanceOf[java.lang.Integer] &&
        !arg.isInstanceOf[java.lang.Long] &&
        !arg.isInstanceOf[java.lang.Float] &&
        !arg.isInstanceOf[java.lang.Double] &&
        !arg.isInstanceOf[java.lang.Boolean] &&
        !arg.isInstanceOf[java.lang.Character] &&
        !arg.getClass.isAnnotationPresent(Annotations.immutable)) {
        hasMutableArgument = true
      }
      if (arg.getClass.getName.contains("$$ProxiedByAWSubclassing$$")) unserializable = true
    }
    if (!unserializable && hasMutableArgument) {
      // FIXME: can we have another default deep cloner?
      val copyOfArgs = Serializer.Java.deepClone(args)
      joinPoint.getRtti.asInstanceOf[MethodRtti].setParameterValues(copyOfArgs.asInstanceOf[Array[AnyRef]])
    }    
  }
}

/*
ublic class CamelInvocationHandler implements InvocationHandler {
     private final Endpoint endpoint;
    private final Producer producer;
    private final MethodInfoCache methodInfoCache;

    public CamelInvocationHandler(Endpoint endpoint, Producer producer, MethodInfoCache methodInfoCache) {
        this.endpoint = endpoint;
        this.producer = producer;
        this.methodInfoCache = methodInfoCache;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        BeanInvocation invocation = new BeanInvocation(method, args);
        ExchangePattern pattern = ExchangePattern.InOut;
        MethodInfo methodInfo = methodInfoCache.getMethodInfo(method);
        if (methodInfo != null) {
            pattern = methodInfo.getPattern();
        }
        Exchange exchange = new DefaultExchange(endpoint, pattern);
        exchange.getIn().setBody(invocation);

        producer.process(exchange);
        Throwable fault = exchange.getException();
        if (fault != null) {
            throw new InvocationTargetException(fault);
        }
        if (pattern.isOutCapable()) {
            return exchange.getOut().getBody();
        } else {
            return null;
        }
    }
}

      if (joinpoint.target.isInstanceOf[MessageDriven] &&
          joinpoint.method.getName == "onMessage") {
        val m = joinpoint.method

      val endpointName = m.getDeclaringClass.getName + "." + m.getName
        val activeObjectName = m.getDeclaringClass.getName
        val endpoint = conf.getRoutingEndpoint(conf.lookupUriFor(m))
        val producer = endpoint.createProducer
        val exchange = endpoint.createExchange
        exchange.getIn().setBody(joinpoint)
        producer.process(exchange)
        val fault = exchange.getException();
        if (fault != null) throw new InvocationTargetException(fault)

        // FIXME: need some timeout and future here...
        exchange.getOut.getBody

      } else
*/
