package com.company.camel

import java.util.Date

import akka.actor.{ActorLogging, ActorSystem, Props, Status}
import akka.camel._
import akka.util.Timeout
import com.fasterxml.jackson.core.JsonParseException
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.language.postfixOps

/**
 *
 */
object Main extends App with LazyLogging with Configuration {

  implicit val actorSystem = ActorSystem(actorSystemName)
  implicit val contextExecutor = actorSystem.dispatcher
  val camel = CamelExtension(actorSystem)

  implicit val timeout = Timeout(6 seconds)

  val producerActor = actorSystem.actorOf(Props(new SimpleProducer(endPointQueueFeatureTest)), name = "simpleProducer")
  val consumerActor = actorSystem.actorOf(Props(new SimpleConsumer(endPointQueueFeatureTest)), name = "simpleConsumer")

  // get a future reference to the activation of the endpoint of the Consumer Actor
  val activationFuture = camel.activationFutureFor(consumerActor)(timeout = 20 seconds, executor = contextExecutor)

  logger.info("Push message")
  producerActor ! "fake message:0"

  for (i <- 1 to 10) {
    producerActor ! s"fake message:$i"
    Thread.sleep(1000)
  }

}

class SimpleProducer(_endpointUri: String) extends Oneway with LazyLogging {
  override def endpointUri: String = _endpointUri

  override def preStart() {
    super.preStart()
    logger.info(s"SimpleProducer pre start at:${new Date()}")

    Thread.sleep(2000)
    logger.info(s"MessageProducer pre ended at:${new Date()}")

    val _paths = context.actorSelection("akka://ActorAfterFeature/user/*")
    logger.info(s"actor selection:${_paths}")
  }

  def upperCase(msg: CamelMessage) = msg.mapBody {
    body: String => body.toUpperCase
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
        upperCase(msg)
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

class SimpleConsumer(_endpointUri: String) extends Consumer with ActorLogging {

  override def autoAck = false

  override def endpointUri: String = _endpointUri

  var counter: Long = 0L


  override def receive = {
    case msg: CamelMessage =>
      try {
        val message = msg.bodyAs[String]
        implicit val timeout = Timeout(6 seconds)

        counter += 1
        log.info(s"Counter:$counter")
        log.info(s"Consumed a message:$message\n")
        if (counter % 5 == 0) {
          //          throw new Exception("Fake exception")
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
