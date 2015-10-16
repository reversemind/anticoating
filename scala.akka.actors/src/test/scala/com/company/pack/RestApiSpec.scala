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

    "return a default for GET requests to the root path" in {
      Get() ~> restRoute ~> check { responseAs[String] must contain("VALUE") }
    }

    "return a 'VALUE' response for GET requests to /entity/VALUE" in {
      Get("entity/VALUE") ~> restRoute ~> check { responseAs[String] === "VALUE" }
    }

//    "leave GET requests to other paths unhandled" in {
//      Get("/kermit") ~> demoRoute ~> check { handled must beFalse }
//    }

//    //# source-quote (for the documentation site)
//    "return a MethodNotAllowed error for PUT requests to the root path" in {
//      Put() ~> sealRoute(demoRoute) ~> check {
//        status === MethodNotAllowed
//        responseAs[String] === "HTTP method not allowed, supported methods: GET, POST"
//      }
//    }

    //#
  }
}
