package com.company.pack

import akka.actor.ActorLogging
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import spray.routing.{HttpService, HttpServiceActor}
import spray.util._

/**
 *
 */
class RestApiActor(_requestTimeout: Timeout) extends HttpServiceActor with RestApi with ActorLogging {

  override def actorRefFactory = context

  implicit val requestTimeout = _requestTimeout

  def receive = runRoute(rootRoute)
}

trait RestApi extends HttpService with LazyLogging {

  // we use the enclosing ActorContext's or ActorSystem's dispatcher for our Futures and Scheduler
  implicit def executionContext = actorRefFactory.dispatcher

  val rootRoute = {
    get {
      pathSingleSlash {
        complete("DEFAULT_GET_VALUE")
      } ~
        path("value" / Segment) { id =>
          complete(s"get:$id")
        }
    } ~
      post {
        complete("DEFAULT_POST_VALUE")
      } ~
      // shutdown actors by command
      (put | parameter('method ! "put")) {
        path("stop") {
          complete {
            actorSystem.shutdown()
            "Shutting down in 1 second..."
          }
        }
      }

  }
}

