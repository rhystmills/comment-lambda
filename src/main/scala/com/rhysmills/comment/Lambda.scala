package com.rhysmills.comment

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}

class Lambda {
  def handleRequest(event: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    new APIGatewayProxyResponseEvent()
      .withStatusCode(200)
      .withBody("okay")
  }
}
