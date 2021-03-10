package com.rhysmills.comment

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}

import scala.jdk.CollectionConverters.MapHasAsScala


class Lambda {
  def handleRequest(event: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    val httpMethod = event.getHttpMethod()
    val queryList = Option(event.getQueryStringParameters()) match {
      case Some(javaQueryList) => javaQueryList.asScala.toList
      case None => Nil
    }
    val queryText = queryList.map(pair => "\n" + pair._1 + ": " + pair._2).mkString("")
    val text = httpMethod + "\n" + "Query strings: " + queryText
    new APIGatewayProxyResponseEvent()
      .withStatusCode(200)
      .withBody(text)
  }
}
