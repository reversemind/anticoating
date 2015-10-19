package com.company.config

import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}

import scala.util.Try

/**
 *
 */
trait Configuration extends RequestTimeout {

  val config = ConfigFactory.load()

  lazy val host = Try(config.getString("http.host")).getOrElse("127.0.0.1")
  lazy val port = Try(config.getInt("http.port")).getOrElse(8080)

  lazy implicit val requestChunkAggregationLimit = Try(config.getString("spray.can.server.request-chunk-aggregation-limit")).getOrElse("10485760").toLong

  lazy implicit val timeout = requestTimeout(config)

}

trait RequestTimeout {

  import scala.concurrent.duration._

  def requestTimeout(config: Config): Timeout = {
    val t = config.getString("spray.can.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}
