package com.company.afterfutures

import java.util.{Date, UUID}

import akka.actor.Actor
import akka.event.Logging
import akka.io.Tcp.Register
import com.company.afterfutures.MessageProducer.Generate
import com.typesafe.scalalogging.LazyLogging

/**
 *
 */
object MessageProducer{
  case object Generate
}

class MessageProducer extends Actor with LazyLogging {
  val log = Logging(context.system, this)

  override def preStart() {
    // registering with other actors
    log.info(s"pre start at:${new Date()}")
    Register(self)
  }

  override def receive = {
    case Generate => {
      val _message = "message:" + new Date() + "|" + UUID.randomUUID().toString
      log.info(s"Going to send a message:" + _message)
      sender() ! _message
    }
    case "test" => log.info("received test")
    case _ => log.info("received unknown message")
  }
}
