package com.company.pack

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 *
 */
object Main extends App {

  implicit val system = ActorSystem("ScalaActorExplorer")
  implicit val executionContext = system.dispatcher

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  val _t = config.getString("spray.can.server.request-timeout")
  implicit val requestTimeout = FiniteDuration(Duration(_t).length, Duration(_t).unit)

  val api = system.actorOf(Props(new RestApi(requestTimeout)), "httpInterface")
}
