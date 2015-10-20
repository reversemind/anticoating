package com.company.afterfutures

import java.util.Date

import akka.actor._
import akka.event.Logging
import akka.io.Tcp.Register

/**
 *
 */
object QueueApi {

  case class GetMessage(text: String)

  case object Cancel

}

class QueueApi extends Actor {

  import QueueApi._

  val log = Logging(context.system, this)

  override def preStart() {
    // registering with other actors
    log.info(s"pre start at:${new Date()}")
    Register(self)
  }

  override def receive = {
    case "test" => log.info("received test")
    case GetMessage(text) => sender() ! s"back message:$text"
    case Cancel => log.info("event:Cancel")
    case _ => log.info("received unknown message")
  }
}

