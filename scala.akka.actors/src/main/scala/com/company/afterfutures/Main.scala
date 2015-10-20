package com.company.afterfutures

import java.util.Date

import akka.actor.{ActorSystem, Props}
import akka.camel.CamelMessage
import com.typesafe.scalalogging.LazyLogging
import org.apache.camel.component.rabbitmq.RabbitMQConstants

/**
 *
 */
object Main extends App with LazyLogging {

  implicit val actorSystem = ActorSystem("ActorAfterFeature")
  implicit val context = actorSystem.dispatcher

//  val endPoint = "prefetch.queue"
  val endPoint = "rabbitmq://localhost:5672/prefetch.queue?autoAck=false&autoDelete=true&automaticRecoveryEnabled=true"

  val queueService = actorSystem.actorOf(Props(new QueueApi()), "queueActor")
  val producerService = actorSystem.actorOf(Props(new MessageProducer()), "messageProducerActor")

  val messageConsumerActor = actorSystem.actorOf(Props(classOf[MessageConsumer]))
  val messageProducerActor = actorSystem.actorOf(Props(classOf[CamelProducer], endPoint))

  logger.info(s"Started main at :${new Date()}")

  val message = new CamelMessage(
    MessageProducer.Generate,
    Map(RabbitMQConstants.ROUTING_KEY -> "prefetched.queue")
  )

  messageProducerActor ! message
}
