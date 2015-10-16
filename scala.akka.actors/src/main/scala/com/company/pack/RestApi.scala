package com.company.pack

import akka.actor.Actor.Receive
import akka.util.Timeout
import spray.routing.HttpServiceActor

/**
 *
 */
class RestApi(_requestTimeout: Timeout) extends HttpServiceActor{

  implicit val requestTimeout = _requestTimeout

  def receive = runRoute(null)
}