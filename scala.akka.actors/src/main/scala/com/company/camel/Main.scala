package com.company.camel

import akka.actor._
import akka.camel._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.apache.camel.component.rabbitmq.RabbitMQConstants
import org.apache.camel.model.language.ConstantExpression
import org.apache.camel.model.{ProcessorDefinition, RouteDefinition}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Random, Success}

/**
 *
 */
object Main extends App with LazyLogging with Configuration {

  implicit val actorSystem = ActorSystem(actorSystemName, ConfigFactory.parseString("""
                                                                                      akka {
                                                                                        actor {
                                                                                          timeout = 20
                                                                                        }
                                                                                      }
                                                                                    """))
  implicit val contextExecutor = actorSystem.dispatcher
  val camel = CamelExtension(actorSystem)

  implicit val timeout = Timeout(6 seconds)

  val producerActor = actorSystem.actorOf(Props(new SimpleProducer(endPointQueueFeatureTest)), name = "simpleProducer")
  val consumerActor = actorSystem.actorOf(Props(new SimpleConsumer(endPointQueueFeatureTest)), name = "simpleConsumer")

  logger.info("Push messages")

  for (i <- 1 to 4) {
    producerActor ! s"fake message:$i"
    Thread.sleep(100)
  }

}

class SimpleProducer(_endpointUri: String) extends Oneway with LazyLogging {
  override def endpointUri: String = _endpointUri
}

class SimpleConsumer(_endpointUri: String) extends Consumer with ActorLogging {

  import context.dispatcher

  val MAX_RETRIES = 3
  val RETRY_DELAY = 5 seconds

  override def autoAck = false

  implicit val timeout = Timeout(20 seconds)

  override def endpointUri: String = _endpointUri

  var counter: Long = 0L
  var retryCounter = 0L

  val postActor = context.actorOf(Props(PostActor))

  override def onRouteDefinition: (RouteDefinition) => ProcessorDefinition[_] =
    (rd) => rd.setHeader(RabbitMQConstants.REQUEUE, new ConstantExpression("true"))

  /**
   *
   * @return
   */
  override def receive = {
    case msg: CamelMessage => {

      val _sender = sender()
      val _self = self

      val message = msg.bodyAs[String]

      counter += 1

      val delayed = akka.pattern.after(RETRY_DELAY, using = context.system.scheduler)({
        retryCounter += 1
        if(retryCounter == 10){
          context.stop(_self)
        }
        Future(_sender ! Status.Failure(new RuntimeException("RuntimeException №1")))
        }
      )

      lazy val future = Future firstCompletedOf Seq(postActor ? message, delayed)

      future.onComplete {
        case Success(notification) => notification match {
            case "NOT_OK" => {
              log.info(s"!!! IS NOT Success !!! = Message is NOT POSTed:'$notification'")
//              _sender ! Status.Failure(new RuntimeException("RuntimeException #3"))
            }
            case _ => {
              log.info(s"!!! Success !!! = Message is POSTed:'$notification'")
              _sender ! Ack
            }
          }
        case Failure(ex) =>
          log.error(s"!!! Failure !!! = Message is NOT POSTed", ex)
          //          context.stop(postActor)
          _sender ! Status.Failure(ex)
      }
    }
  }
}

object PostActor extends Actor with LazyLogging {

  var counter: Long = 0

  override def receive: Receive = {
    case message: String => {

      counter += 1
      val sleepFor = new Random().nextInt(1 * 3000)
      logger.info(s"\nCounter is:$counter and will sleep for:$sleepFor ms with message:'$message'\n")

//      Thread.sleep(sleepFor)

      //      if(message.equals("fake message:3")){
      //        logger.info(s"\nUnable to send a POST for message:'$message' let's try again for POST counter:$counter\n")
      //        sender() ! Status.Failure(new RuntimeException(s"Exception - unable to send a POST for $message"))
      //      }else if (counter % 2 == 0) {
      //        logger.info(s"\nUnable to send a POST for message:'$message' let's try again for POST counter:$counter\n")
      //        sender() ! Status.Failure(new RuntimeException("Exception - unable to send a POST"))
      //      } else {
      //        logger.info(s"\nPOST was successfully sent for message:'$message' - for POST counter:$counter\n")
      //        sender() ! s"\nPOST was successfully sent for message:'$message'\n"
      //      }

      if (message != null) {
        if (message.equals("fake message:3")) {
          logger.info(s"\nUnable to send a POST for message:'$message' let's try again for POST counter:$counter\n")
          sender() ! "NOT_OK"; //Status.Failure(new IllegalArgumentException(s"IllegalArgumentException - unable to send a POST for $message"))
        } else {
          logger.info(s"\nPOST was successfully sent for message:'$message' - for POST counter:$counter\n")
          sender() ! s"\nPOST was successfully sent for message:'$message'\n"
        }
      }

    }

  }

}
