package com.company.camel

import com.typesafe.config.ConfigFactory

import scala.util.Try

/**
 *
 */
trait Configuration {

  val config = ConfigFactory.load()

  lazy implicit val actorSystemName = "ActorAfterFeature"

  lazy val endPointQueueFeatureTest = Try(config.getString("camel.rabbitmq.endPointQueueFeatureTest")).getOrElse("")
}
