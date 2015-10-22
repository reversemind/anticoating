package com.company.camel

import akka.actor._
import akka.camel._
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.duration._
import scala.concurrent.{Future, TimeoutException}
import scala.language.postfixOps
import scala.util.{Failure, Random, Success}

/**
 *
 */
object Main extends App with LazyLogging with Configuration {

  implicit val actorSystem = ActorSystem(actorSystemName)
  implicit val contextExecutor = actorSystem.dispatcher
  val camel = CamelExtension(actorSystem)

  implicit val timeout = Timeout(6 seconds)

  val MAX_RETRIES = 3
  val RETRY_DELAY = 5 seconds

  val producerActor = actorSystem.actorOf(Props(new SimpleProducer(endPointQueueFeatureTest)), name = "simpleProducer")
  val consumerActor = actorSystem.actorOf(Props(new SimpleConsumer(endPointQueueFeatureTest)), name = "simpleConsumer")


  // get a future reference to the activation of the endpoint of the Consumer Actor
//  val activationFuture = camel.activationFutureFor(consumerActor)(timeout = 20 seconds, executor = contextExecutor)

  logger.info("Push messages")
//  producerActor ! "fake message:0"

  for (i <- 1 to 4) {
    producerActor ! s"fake message:$i"
    Thread.sleep(10)
  }

}

class SimpleProducer(_endpointUri: String) extends Oneway with LazyLogging {

  override def endpointUri: String = _endpointUri
//
//  def upperCase(msg: CamelMessage) = msg.mapBody {
//    body: String => body.toUpperCase
//  }
//
//  override def routeResponse(msg: Any): Unit = sender ! transformResponse(msg)
//
//  override def transformOutgoingMessage(message: Any): Any = {
//    message match {
//      case msg: CamelMessage => {
//        try {
//          val content = msg.bodyAs[String]
//        } catch {
//          case ex: Exception =>
//            "TransformException: %s".format(ex.getMessage)
//        }
//        msg
//      }
//      case other =>
//        message
//    }
//  }
//
//  override def transformResponse(message: Any): Any = {
////    logger.info(s"Produce a message:$message")
//    message match {
//      case msg: CamelMessage => {
//        try {
//          val content = msg.bodyAs[String]
//          msg
//        } catch {
//          case ex: Exception =>
//            "TransformException: %s".format(ex.getMessage)
//        }
//      }
//      case other => message
//    }
//  }

}

class SimpleConsumer(_endpointUri: String) extends Consumer with ActorLogging {

  import context.dispatcher

  override def autoAck = false
  implicit val timeout = Timeout(20 seconds)
  override def endpointUri: String = _endpointUri
  var counter: Long = 0L

  override def receive = {
    case msg: CamelMessage => {
      val _sender = sender()
      val _self = self

      val message = msg.bodyAs[String]

      counter += 1

      val postActor = context.actorOf(Props(PostActor))

      val delayed = akka.pattern.after(5 seconds, using = context.system.scheduler)(
//      {
//        log.info(s"Let's try send it again:$message")
//        Future(_self ! message)
//      }
//          log.info(s"Let's try send it again:$message")
//        Future(_self ! message)
        Future(_sender ! Status.Failure(new RuntimeException("============================RuntimeException")))
//        Future(_sender ! Status.Failure(new RuntimeException("Future timeouted")))
//          Future.failed(new RuntimeException("Future timeouted"))
      )

      val future = Future firstCompletedOf Seq(postActor ? message, delayed)

      future onComplete {
        case Success(notification) =>
          log.info(s"!!! Success !!! = Message is POSTed:$notification")
          _sender ! Ack
        case Failure(ex) =>
          log.error(s"!!! Failure !!! = Message is NOT POSTed", ex)
          _sender ! Status.Failure(ex)
      }

//      future pipeTo sender()
    }
    case other =>
      log.info(s"Message:$other")
      sender() ! Ack
  }
}

object PostActor extends Actor with LazyLogging {

  var counter: Long = 0

  override def receive: Receive = {
    case message: String => {

      counter += 1
      val sleepFor = new Random().nextInt(1 * 1000)
      logger.info(s"\nCounter is:$counter and will sleep for:$sleepFor ms with message:'$message'\n")

//      Thread.sleep(sleepFor)
      if (counter % 2 == 0) {
        logger.info(s"\nUnable to send a POST for message:'$message' let's try again for POST counter:$counter\n")
        sender() ! Status.Failure(new RuntimeException("Exception - unable to send a POST"))
      } else {
        logger.info(s"\nPOST was successfully sent for message:'$message' - for POST counter:$counter\n")
        sender() ! s"\nPOST was successfully sent for message:'$message'\n"
      }
    }

  }

}
