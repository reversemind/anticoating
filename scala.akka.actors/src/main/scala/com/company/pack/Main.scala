package com.company.pack

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.util.Timeout
import akka.pattern.ask
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import spray.can.Http

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

/**
 *
 */
object Main extends App with RequestTimeout with ShutdownIfNotBound{

  implicit val actorSystem = ActorSystem("ScalaActorExplorer")
  implicit val executionContext = actorSystem.dispatcher

  val config = ConfigFactory.load()
  val host = Try(config.getString("http.host")).getOrElse("127.0.0.1")
  val port = Try(config.getInt("http.port")).getOrElse(8080)

  implicit val timeout = requestTimeout(config)
  val apiService = actorSystem.actorOf(Props(new RestApiActor(timeout)), "httpInterface")

  val response = IO(Http).ask(Http.Bind(listener = apiService, interface = host, port = port))
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
