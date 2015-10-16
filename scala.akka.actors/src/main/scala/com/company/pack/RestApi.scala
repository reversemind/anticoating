package com.company.pack

import akka.actor.ActorLogging
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import spray.routing.{HttpService, HttpServiceActor}

/**
 *
 */
class RestApiActor(_requestTimeout: Timeout) extends HttpServiceActor with RestApi with ActorLogging {

  implicit val requestTimeout = _requestTimeout

  def receive = runRoute(restRoute)
}

trait RestApi extends HttpService with LazyLogging{
  val restRoute = {
    path("entity" / Segment) { id =>
      logger.info(s"input value:$id")
      get {
        pathSingleSlash {
          complete(s"get:$id")
        } ~
          path("ping") {
            complete("PONG!")
          }
      } ~
      post {
        complete(s"post:$id")
      }
    }
  }
}

