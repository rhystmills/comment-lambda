package com.rhysmills.comment

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.rhysmills.comment.models.Serialisers.commentEncoder
import com.rhysmills.comment.models.{Comment, Failure}
import io.circe.syntax._
import org.scanamo.generic.auto._
import org.scanamo.syntax._
import org.scanamo.{Scanamo, Table}
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

import scala.jdk.CollectionConverters.MapHasAsScala
import scala.util.Properties

// GET request includes an article ID - its url would work. Response includes comments for the article.
// POST request includes a comment to add. This would have strings (name, comment, time/date), a parent comment, and its article

class Lambda {
  val region = Properties.envOrNone("region").get // TODO: Do this better
  val tableName = Properties.envOrNone("tableName").get // TODO: Do this better
  val dbClient = DynamoDbClient.builder()
    .region(Region.of(region))
    .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
    .build()

  def handleRequest(event: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {

    val queryMap = Option(event.getQueryStringParameters) match {
      case Some(javaQueryMap) => javaQueryMap.asScala.toMap
      case None => Map.empty[String, String]
    }

    Comments.myActualProgram(queryMap, context, dbClient, tableName)
  }
}

object Comments {
  def myActualProgram(queryMap: Map[String, String], context: Context, dbClient: DynamoDbClient, tableName: String) = {
    val commentsTable = Table[Comment](tableName)

    val result = for {
      articleId <- queryMap.get("articleId").toRight(Failure("articleId parameter is required", "articleId parameter is required", 400))
      commentsEither <- Scanamo(dbClient).exec(commentsTable.get("articlePath" === articleId)).toRight(Failure("Comment not found", "Comment not found", 404))
      comment <- commentsEither.left.map(error => Failure(error.toString, "Server Error", 500))
    } yield comment

    result.fold(
      { failure =>
        context.getLogger.log(failure.logMessage)
        new APIGatewayProxyResponseEvent()
          .withStatusCode(failure.statusCode)
          .withBody(failure.friendlyMessage)
      },
      { body =>
        new APIGatewayProxyResponseEvent()
          .withStatusCode(200)
          .withBody(body.asJson.noSpaces)
      }
    )

  }
}

