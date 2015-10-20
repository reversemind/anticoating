package com.company.afterfutures

import akka.camel.Ack
import akka.actor.Actor.Receive
import akka.actor.{Status, Props}
import akka.camel.{CamelMessage, Consumer}
import akka.util.Timeout
import com.fasterxml.jackson.core.JsonParseException
import com.typesafe.scalalogging.LazyLogging
import org.apache.camel.model.language.ConstantExpression
import org.apache.camel.model.{ProcessorDefinition, RouteDefinition}
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 *
 */
class MessageConsumer extends MQConsumer with LazyLogging{

  import context.dispatcher

  override def endpointUri: String = "prefetch.queue"

  override def receive= {
    case msg: CamelMessage =>
      try {
        val message = msg.bodyAs[String]
        logger.info(s"Processing task $message")

        implicit val timeout = Timeout(30 seconds)
      } catch {
        case e: JsonParseException =>
          logger.warn("Bad message format, ignoring", e)
          sender ! Ack

        case e: Exception => sender ! Status.Failure(e)
      }
  }
}

trait MQConsumer extends Consumer {
  override def autoAck = false

  override def onRouteDefinition: (RouteDefinition) => ProcessorDefinition[_] =
    (rd) => rd.setHeader("rabbitmq.REQUEUE", new ConstantExpression("true"))
}
