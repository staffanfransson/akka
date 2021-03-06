package se.scalablesolutions.akka.actor

import org.scalatest.junit.JUnitSuite
import org.junit.Test

class MemoryFootprintTest extends JUnitSuite   {
  class Mem extends Actor {
    def receive = {
      case _ => {}
    }
  }

  val NR_OF_ACTORS = 100000
  val MAX_MEMORY_FOOTPRINT_PER_ACTOR = 600

  @Test
  def actorsShouldHaveLessMemoryFootprintThan630Bytes = {
    println("============== MEMORY FOOTPRINT TEST ==============")
    // warm up
    (1 until 10000).foreach(i => new Mem)

    // Actors are put in AspectRegistry when created so they won't be GCd here

    val totalMem = Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory
    (1 until NR_OF_ACTORS).foreach(i => new Mem)
    
    val newTotalMem = Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory
    val memPerActor = (newTotalMem - totalMem) / NR_OF_ACTORS

    println("Memory footprint per actor is : " + memPerActor)
    assert(memPerActor < MAX_MEMORY_FOOTPRINT_PER_ACTOR) // memory per actor should be less than 630 bytes
  }
}
