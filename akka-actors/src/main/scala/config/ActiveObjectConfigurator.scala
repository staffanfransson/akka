/**
 * Copyright (C) 2009 Scalable Solutions.
 */

package se.scalablesolutions.akka.config

import JavaConfig._

import com.google.inject._

import java.util._
//import org.apache.camel.impl.{JndiRegistry, DefaultCamelContext}
//import org.apache.camel.{Endpoint, Routes}

/**
 * Configurator for the Active Objects. Used to do declarative configuration of supervision.
 * It also does dependency injection with and into Active Objects using dependency injection
 * frameworks such as Google Guice or Spring.
 * <p/>
 * If you don't want declarative configuration then you should use the <code>ActiveObject</code>
 * factory methods.
 * 
 * @author <a href="http://jonasboner.com">Jonas Bon&#233;r</a>
 */
class ActiveObjectConfigurator {
  // TODO: make pluggable once we have f.e a SpringConfigurator
  private val INSTANCE = new ActiveObjectGuiceConfigurator

  /**
   * Returns the active abject that has been put under supervision for the class specified.
   *
   * @param clazz the class for the active object
   * @return the active object for the class
   */
  def getInstance[T](clazz: Class[T]): T = INSTANCE.getInstance(clazz)

  def configure(restartStrategy: RestartStrategy, components: Array[Component]): ActiveObjectConfigurator = {
    INSTANCE.configure(
      restartStrategy.transform,
      components.toList.asInstanceOf[scala.List[Component]].map(_.transform))
    this
  }

  def inject: ActiveObjectConfigurator = {
    INSTANCE.inject
    this
  }

  def supervise: ActiveObjectConfigurator = {
    INSTANCE.supervise
    this
  }

  def addExternalGuiceModule(module: Module): ActiveObjectConfigurator = {
    INSTANCE.addExternalGuiceModule(module)
    this
  }

  //def addRoutes(routes: Routes): ActiveObjectConfigurator  = {
  //  INSTANCE.addRoutes(routes)
  //  this
 // }

  
  def getComponentInterfaces: List[Class[_]] = {
    val al = new ArrayList[Class[_]]
    for (c <- INSTANCE.getComponentInterfaces) al.add(c)
    al
  }

  def getExternalDependency[T](clazz: Class[T]): T = INSTANCE.getExternalDependency(clazz)

  //def getRoutingEndpoint(uri: String): Endpoint = INSTANCE.getRoutingEndpoint(uri)

  //def getRoutingEndpoints: java.util.Collection[Endpoint] = INSTANCE.getRoutingEndpoints

  //def getRoutingEndpoints(uri: String): java.util.Collection[Endpoint] = INSTANCE.getRoutingEndpoints(uri)

  // TODO: should this be exposed?
  def getGuiceModules: List[Module] = INSTANCE.getGuiceModules

  def reset = INSTANCE.reset

  def stop = INSTANCE.stop
}
