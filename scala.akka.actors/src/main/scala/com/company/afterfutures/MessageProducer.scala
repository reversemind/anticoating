package com.company.afterfutures

import java.util.{Date, UUID}

import akka.actor.Actor
import akka.camel.{CamelMessage, Oneway, Producer}
import akka.event.Logging
import akka.io.Tcp.Register
import com.typesafe.scalalogging.LazyLogging

/**
 *
 */
class MessageProducer(_endpointUri: String) extends Producer with Oneway with LazyLogging {

  override def endpointUri: String = _endpointUri

  val log = Logging(context.system, this)

  override def preStart() {
    // registering with other actors
    log.info(s"MessageProducer pre start at:${new Date()}")
    Register(self)
  }

  override def transformResponse(message: Any): Any = { //<co id="ch08-order-producer3-1"/>
    message match {
      case msg: CamelMessage => {
        try {
          val content = msg.bodyAs[String]
          logger.info(s"Produce a message:$content")
          content
        } catch {
          case ex: Exception =>
            "TransformException: %s".format(ex.getMessage)
        }
      }
      case other => message
    }
  }

//  override def receive = {
//    case Generate => {
//      val _message = "message:" + new Date() + "|" + UUID.randomUUID().toString
//      log.info(s"Going to send a message:" + _message)
//      sender() ! _message
//    }
//    case "test" => log.info("received test")
//    case _ => log.info("received unknown message")
//  }

}
