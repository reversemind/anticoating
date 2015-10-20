package com.company.afterfutures

import akka.camel.{CamelMessage, Oneway}
import com.typesafe.scalalogging.LazyLogging

/**
 *
 */
class CamelProducer(endpoint: String) extends Oneway with LazyLogging {
  def endpointUri = endpoint

  override def routeResponse(msg: Any): Unit = sender ! transformResponse(msg)

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
