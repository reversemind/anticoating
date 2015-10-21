package com.company.camel

import java.util.Date

import akka.actor.Actor.Receive
import akka.actor.{ActorLogging, Status, ActorSystem, Props}
import akka.camel.{Ack, CamelMessage, Consumer, Oneway}
import akka.io.Tcp.Register
import akka.util.Timeout
import com.fasterxml.jackson.core.JsonParseException
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 *
 */
object Main extends App with LazyLogging{

  implicit val actorSystem = ActorSystem("ActorAfterFeature")
  implicit val context = actorSystem.dispatcher

  implicit val timeout = Timeout(6 seconds)

  val endPoint = "rabbitmq://localhost:5672/exchangePrefetch?queue=prefetch.queue&autoAck=false&autoDelete=false&automaticRecoveryEnabled=true&exchangeType=topic&routingKey=routingKey"

  val producer = actorSystem.actorOf(Props(new SimpleProducer(endPoint)), name = "simpleProducer")
  val consumer = actorSystem.actorOf(Props(new SimpleConsumer(endPoint)), name = "simpleConsumer")

  logger.info("Push message")
  producer ! "fake message:0"

  for(i <-1 to 10){
    producer ! s"fake message:$i"
    Thread.sleep(1000)
  }

}

class SimpleProducer(_endpointUri: String) extends Oneway with LazyLogging{
  override def endpointUri: String = _endpointUri

  override def preStart() {
    super.preStart()
    logger.info(s"SimpleProducer pre start at:${new Date()}")

    Thread.sleep(2000)
    logger.info(s"MessageProducer pre ended at:${new Date()}")

    val _paths = context.actorSelection("akka://ActorAfterFeature/user/*")
    logger.info(s"actor selection:${_paths}")
  }

  override def transformOutgoingMessage(message: Any): Any = {
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

  override def transformResponse(message: Any): Any = {
    message match {
      case msg: CamelMessage => {
        try {
          val content = msg.bodyAs[String]
          logger.info(s"Produce a message:$content")
          msg
        } catch {
          case ex: Exception =>
            "TransformException: %s".format(ex.getMessage)
        }
      }
      case other => message
    }
  }

}

class SimpleConsumer(_endpointUri: String) extends Consumer with ActorLogging{

  override def autoAck = false

  override def endpointUri: String = _endpointUri

  var counter: Long = 0L

  override def receive= {
    case msg: CamelMessage =>
      try {
        val message = msg.bodyAs[String]
        implicit val timeout = Timeout(6 seconds)

        counter += 1
        log.info(s"Counter:$counter")
        log.info(s"Consumed a message:$message\n")
        if(counter % 5 == 0){
          throw new Exception("Fake exception")
        }

        sender ! Ack
      } catch {
        case e: JsonParseException =>
          log.debug("Bad message format, ignoring", e)
          sender ! Ack

        case e: Exception =>
          log.error("Unknown error:", e)
          sender ! Status.Failure(e)
      }
    case other =>
      log.info(s"Message:$other")
      sender ! Ack
  }
}
