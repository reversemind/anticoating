package com.company.afterfutures

import akka.camel.Oneway

/**
 *
 */
class CamelProducer (endpoint: String) extends Oneway {
  def endpointUri = endpoint

  override def routeResponse(msg: Any): Unit = sender ! transformResponse(msg)
}
