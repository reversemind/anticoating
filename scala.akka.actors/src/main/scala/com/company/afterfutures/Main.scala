package com.company.afterfutures

import java.util.Date

import akka.pattern.{ask, pipe}
import akka.actor.{ActorSystem, Props}
import akka.camel.CamelMessage
import akka.util.Timeout
import com.typesafe.scalalogging.LazyLogging
import org.apache.camel.component.rabbitmq.RabbitMQConstants
import scala.concurrent.duration._
import scala.language.postfixOps

/**
 *
 */
object Main extends App with LazyLogging {

  implicit val actorSystem = ActorSystem("ActorAfterFeature")
  implicit val context = actorSystem.dispatcher

  implicit val timeout = Timeout(6 seconds)

  val endPoint = "rabbitmq://localhost:5672/exchangePrefetch?queue=prefetch.queue&autoAck=false&autoDelete=false&automaticRecoveryEnabled=true&exchangeType=topic&routingKey=routingKey"

  val queueService = actorSystem.actorOf(Props(new QueueApi()), "queueActor")
  val producerService = actorSystem.actorOf(Props(new MessageProducer(endPoint)), "messageProducerActor")

  val messageConsumerActor = actorSystem.actorOf(Props(new MessageConsumer(endPoint)), "messageConsumerActor")
  val messageProducerActor = actorSystem.actorOf(Props(classOf[CamelProducer], endPoint), "camelProducerActor")

  logger.info(s"Started main at :${new Date()}")

  val message = new CamelMessage(
    "\"Message from Actor\"",
    Map(RabbitMQConstants.ROUTING_KEY -> "prefetched.queue")
  )

  // http://doc.akka.io/docs/akka/snapshot/scala/actors.html

//  Thread.sleep(5000)
//  var a = 0
//  for( a <- 1 until 10){
//    logger.info(s"a:$a")
//    producerService ! message
//  }

  val feature = producerService ! message
}
