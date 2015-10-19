package com.company.pack

import akka.actor.{ActorSystem, Props}
import akka.io.IO
import akka.pattern.ask
import com.typesafe.scalalogging.LazyLogging
import spray.can.Http

/**
 *
 */
object Main extends App with ShutdownIfNotBound with com.company.config.Configuration {

  implicit val actorSystem = ActorSystem("ScalaActorExplorer")
  implicit val executionContext = actorSystem.dispatcher

  val apiService = actorSystem.actorOf(Props(new RestApiActor(timeout)), "httpInterface")

  val response = IO(Http).ask(Http.Bind(listener = apiService, interface = host, port = port))

  shutdownIfNotBound(response)
}

trait ShutdownIfNotBound extends LazyLogging {

  import scala.concurrent.{ExecutionContext, Future}

  def shutdownIfNotBound(f: Future[Any])(implicit system: ActorSystem, ec: ExecutionContext) = {
    f.mapTo[Http.Event].map {
      case Http.Bound(address) =>
        logger.info(s"REST interface bound to $address")

      case Http.CommandFailed(cmd) =>
        logger.info(s"REST interface could not bind: ${cmd.failureMessage}, shutting down.")
        system.shutdown()

    }.recover {
      case e: Throwable =>
        logger.info(s"Unexpected error binding to HTTP: ${e.getMessage}, shutting down.")
        system.shutdown()
    }
  }
}
