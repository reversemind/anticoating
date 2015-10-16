package com.company.pack

import akka.actor.ActorRefFactory
import org.specs2.mutable.Specification
import spray.testkit.Specs2RouteTest
import spray.http._
import StatusCodes._

/**
 *
 */
class RestApiSpec extends Specification with Specs2RouteTest with RestApi {

  override implicit def actorRefFactory = system

  "The RestApi" should {

    "return a 'DEFAULT_GET_VALUE' for GET requests to the root path" in {
      Get() ~> rootRoute ~> check { responseAs[String] must contain("DEFAULT_GET_VALUE") }
    }

    "return a 'DEFAULT_POST_VALUE' response for POST requests to the root path" in {
      Post() ~> rootRoute ~> check { responseAs[String] === "DEFAULT_POST_VALUE" }
    }

    "return a 'get:DEFAULT' response for GET requests to /value/DEFAULT" in {
      Get("/value/DEFAULT") ~> rootRoute ~> check { responseAs[String] === "get:DEFAULT" }
    }

    "leave GET requests to other paths unhandled" in {
      Get("/unhandledPath") ~> rootRoute ~> check { handled must beFalse }
    }

    "return a MethodNotAllowed error for HEAD requests to the root path" in {
      Head() ~> sealRoute(rootRoute) ~> check {
        status === MethodNotAllowed
        responseAs[String] === "HTTP method not allowed, supported methods: GET, POST, PUT"
      }
    }

    "shutdown a service for PUT request to /stop" in {
      Put("/stop") ~> rootRoute ~> check{ responseAs[String] === "Shutting down in 1 second..."}
    }

  }
}
