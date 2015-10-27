package com.company.camel

import akka.actor._
import akka.camel._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

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


  // get a future reference to the activation of the endpoint of the Consumer Actor
  //  val activationFuture = camel.activationFutureFor(consumerActor)(timeout = 20 seconds, executor = contextExecutor)

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
  val RETRY_DELAY = 2 seconds

  override def autoAck = false

  implicit val timeout = Timeout(20 seconds)

  override def endpointUri: String = _endpointUri

  var counter: Long = 0L

  val postActor = context.actorOf(Props(PostActor))

  override def receive = {
    case msg: CamelMessage => {

      // # stage 2
      //
      //      val message = msg.bodyAs[String]
      //      log.info(s"Income message:$message")
      //      sender() ! Status.Failure(new RuntimeException("RuntimeException"))
      //
      //
      //      counter += 1
      //
      //      if(counter == 2){
      //        context.stop(self)
      //      }


      // # stage 1

      val _sender = sender()
      val _self = self

      val message = msg.bodyAs[String]

      counter += 1

      val delayed = akka.pattern.after(RETRY_DELAY, using = context.system.scheduler)(
        Future(_sender ! Status.Failure(new RuntimeException("RuntimeException")))
      )

      lazy val future = Future firstCompletedOf Seq(postActor ? message, delayed)

      future.onComplete {
        case Success(notification) => notification match {

            case "NOT_OK" => {
              log.info(s"!!! IS NOT Success !!! = Message is NOT POSTed:'$notification'")
              _sender ! Status.Failure(new RuntimeException("RuntimeException #3"))
            }

            case _ => {
              log.info(s"!!! Success !!! = Message is POSTed:'$notification'")
              _sender ! Ack
            }

          }

          //          context.stop(postActor)
        case Failure(ex) =>
          log.error(s"!!! Failure !!! = Message is NOT POSTed", ex)
          //          context.stop(postActor)
          _sender ! Status.Failure(ex)
      }


      //      future onSuccess {
      //        case notification: String => {
      //          log.info(s"!!! Success !!! = Message is POSTed:'$notification'")
      //          context.stop(postActor)
      //          _sender ! Ack
      //        }
      //      }
      //
      //      future onFailure  {
      //        case ex: RuntimeException => {
      //            log.error(s"!!! Failure !!! = Message is NOT POSTed", ex)
      //            context.stop(postActor)
      ////            _sender ! Status.Failure(ex)
      //        }
      //      }

      //      sender() ! Status.Failure(new RuntimeException("RuntimeException #2"))
      ////      future pipeTo sender()
    }
    //    case other =>
    //      log.info(s"Message:$other")
    //      sender() ! Ack
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
