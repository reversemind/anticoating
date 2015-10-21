package com.company.afterfutures

import java.util.{Date, UUID}

import akka.actor.Actor
import akka.camel.javaapi.UntypedProducerActor
import akka.camel.{Ack, CamelMessage, Oneway, Producer}
import akka.event.Logging
import akka.io.Tcp.Register
import com.typesafe.scalalogging.LazyLogging

/**
 *
 */
//class MessageProducer(_endpointUri: String) extends Producer with Oneway with LazyLogging {
class MessageProducer(_endpointUri: String) extends Oneway with LazyLogging {

  override def endpointUri: String = _endpointUri

  val log = Logging(context.system, this)

  val _l = context.actorSelection("akka://ActorAfterFeature/user/*")

  override def preStart() {
    // registering with other actors
    log.info(s"MessageProducer pre start at:${new Date()}")
    Register(self)

    Thread.sleep(2000)
    log.info(s"MessageProducer pre ended at:${new Date()}")

    log.info(s"actor selection:${_l}")
  }

  override def transformResponse(message: Any): Any = {
    message match {
      case msg: CamelMessage => {
        try {
          val content = msg.bodyAs[String]
          logger.info(s"Produce a message:$content")

        } catch {
          case ex: Exception =>
            "TransformException: %s".format(ex.getMessage)
        }
        msg
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
