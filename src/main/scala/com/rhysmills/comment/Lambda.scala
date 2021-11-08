package com.rhysmills.comment

import com.amazonaws.services.lambda.runtime.{Context, LambdaLogger}
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.rhysmills.comment.models.Serialisers.{responseEncoder, commentDecoder}
import com.rhysmills.comment.models.{Comment, CommentsResponse, EmptyResponse, Failure}
import io.circe
import io.circe.syntax._
import org.scanamo.PutReturn.{NewValue, Nothing}
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
  val prodDb = new ProdDB(dbClient, tableName)

  def handleRequest(event: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {

    val queryMap = Option(event.getQueryStringParameters) match {
      case Some(javaQueryMap) => javaQueryMap.asScala.toMap
      case None => Map.empty[String, String]
    }
    Comments.myActualProgram(queryMap, context.getLogger, prodDb, event.getHttpMethod, Option(event.getBody))
  }
}

object Comments {
  def myActualProgram(queryMap: Map[String, String], logger: LambdaLogger, db: DB, httpMethod: String, bodyOpt: Option[String]) = {
//    val commentsTable = Table[Comment](tableName)
    val result = (httpMethod, bodyOpt) match {
      case ("GET", _) => getCommentsForArticleId(queryMap, db)
      case ("POST", Some(body)) => postComment(body, db)
      case ("POST", _) => Left(Failure("POST request has no body", "Submission was empty", 400))
      case (unsupportedMethod, _) => Left(Failure(s"$unsupportedMethod request type not supported", "Request method not supported", 400))
    }

    result.fold(
      { failure =>
        logger.log(failure.logMessage)
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

  def postComment(commentJson: String, db: DB): Either[Failure, EmptyResponse] = {
    for {
      rawComment <- parseCommentJson(commentJson)
      comment <- validateComment(rawComment)
      _ <- db.addComment(comment)
    } yield EmptyResponse()
  }

  def getCommentsForArticleId(queryMap: Map[String, String], db: DB): Either[Failure, CommentsResponse] = {
    for {
      articleId <- queryMap.get("articleId").toRight(Failure("articleId parameter is required", "articleId parameter is required", 400))
      comments <- db.getComments(articleId)
    } yield CommentsResponse(comments)
  }

  def parseCommentJson(commentJson: String): Either[Failure, Comment] = {
    for {
      json <- circe.parser.parse(commentJson).left.map(error => Failure(error.toString, "Comment JSON could not be parsed", 400))
      comment <- json.as[Comment].left.map(error => Failure(error.toString, "Comment structure is incorrect", 400))
    } yield comment
  }

  def validateComment(comment: Comment): Either[Failure, Comment] = {
    if (comment.content.nonEmpty && comment.content.length <= 10000) Right(comment)
    else if (comment.content.isEmpty) Left(Failure("Comment content cannot be empty", "Your comment was empty", 400))
    else Left(Failure(s"Content must be less than 10001 chars: ${comment.content.length} characters", "Your comment was too long", 400))
  }

  def listEitherTraverse[L, A, B](as: List[A])(f: A => Either[L, B]): Either[L, List[B]] = {
    as.foldRight[Either[L, List[B]]](Right(Nil)){ (a, acc) =>
      for {
        bs <- acc
        b <- f(a)
      } yield b :: bs

      // The same:
//      acc match {
//        case Left(l) => Left(l)
//        case Right(bs) => f(a) match {
//          case Left(l) => Left(l)
//          case Right(b) => Right(b :: bs)
//        }
//      }
    }
  }
  def listEitherSequence[L, A](aes: List[Either[L, A]]): Either[L, List[A]] = {
    listEitherTraverse(aes)(identity)
  }
}