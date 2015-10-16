package com.company.pack

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.util.Timeout
import akka.pattern.ask
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import spray.can.Http

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 *
 */
object Main extends App with RequestTimeout with ShutdownIfNotBound with LazyLogging {

  implicit val system = ActorSystem("ScalaActorExplorer")
  implicit val executionContext = system.dispatcher

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  val requestTimeout = requestTimeout(config)
  val api = system.actorOf(Props(new RestApi(requestTimeout)), "httpInterface")

  val response = IO(Http).ask(Http.Bind(listener = api, interface = host, port = port))
  shutdownIfNotBound(response)
}

trait RequestTimeout {

  import scala.concurrent.duration._

  def requestTimeout(config: Config): Timeout = {
    val t = config.getString("spray.can.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}

trait ShutdownIfNotBound {

  import scala.concurrent.{ExecutionContext, Future}

  def shutdownIfNotBound(f: Future[Any])(implicit system: ActorSystem, ec: ExecutionContext) = {
    f.mapTo[Http.Event].map {
      case Http.Bound(address) =>
        println(s"REST interface bound to $address")

      case Http.CommandFailed(cmd) =>
        println(s"REST interface could not bind: ${cmd.failureMessage}, shutting down.")
        system.shutdown()

    }.recover {
      case e: Throwable =>
        println(s"Unexpected error binding to HTTP: ${e.getMessage}, shutting down.")
        system.shutdown()
    }
  }
}
