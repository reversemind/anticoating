package com.company.future

import akka.actor.ActorSystem

import scala.concurrent.Future

/**
 *
 */
object YesFuture extends App {

  implicit val actorSystem = ActorSystem(YesFuture.getClass.getCanonicalName.replaceAll("[^a-zA-Z]", "_"))
  implicit val executorContext = actorSystem.dispatcher

  val startTime = System.currentTimeMillis
  val future1 = Future(timeTakingIdentityFunction(1))
  val future2 = Future(timeTakingIdentityFunction(2))
  val future3 = Future(timeTakingIdentityFunction(3))

  val future = for {
    x <- future1
    y <- future2
    z <- future3
  } yield (x + y + z)

  future onSuccess {
    case sum =>
      val elapsedTime = (System.currentTimeMillis - startTime) / 1000.0
      println("Sum of 1, 2 and 3 is " + sum + " calculated in " + elapsedTime + " seconds")
      actorSystem.shutdown()
  }

  def timeTakingIdentityFunction(number: Int) = {
    // we sleep for 3 seconds and return number
    Thread.sleep(3000)
    number
  }
}
