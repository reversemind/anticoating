package com.company.future

/**
 *
 */
object NoFuture extends App {
  val startTime = System.currentTimeMillis

  val number1 = timeTakingIdentityFunction(1)
  val number2 = timeTakingIdentityFunction(2)
  val number3 = timeTakingIdentityFunction(3)

  val sum = number1 + number2 + number3

  val elapsedTime = (System.currentTimeMillis - startTime) / 1000.0
  println("Sum of 1, 2 and 3 is " + sum + " calculated in " + elapsedTime + " seconds")

  def timeTakingIdentityFunction(number: Int) = {
    // we sleep for 3 seconds and return number
    Thread.sleep(3000)
    number
  }
}
