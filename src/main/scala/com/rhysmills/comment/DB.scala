package com.rhysmills.comment

import com.rhysmills.comment.Comments.listEitherSequence
import com.rhysmills.comment.models.{Comment, CommentsResponse, Failure}
import org.scanamo.PutReturn.Nothing
import org.scanamo.{Scanamo, Table}
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import org.scanamo.generic.auto._
import org.scanamo.syntax._

trait DB {
  def addComment(comment: Comment): Either[Failure, Unit]
  def getComments(articlePath: String): Either[Failure, List[Comment]]
}

class ProdDB (dbClient: DynamoDbClient, tableName: String) extends DB {
  val commentsTable = Table[Comment](tableName)

  override def addComment(comment: Comment): Either[Failure, Unit] = {
    Scanamo(dbClient).exec(commentsTable.putAndReturn(Nothing)(comment))
      .map(_.left.map(dynamoDbError => Failure(dynamoDbError.toString, "Internal database error", 500)))
      .toRight(Failure("Unexpected Database error", "Unexpected Database error", 500)).flatten
      .map(_ => ())
  }

  override def getComments(articlePath: String): Either[Failure, List[Comment]] = {
    val commentsEither = Scanamo(dbClient).exec(commentsTable.query("articlePath" === articlePath))//.toRight(Failure("Comment not found", "Comment not found", 404))
    for {
      comments <- listEitherSequence(commentsEither).left.map(error => Failure(error.toString, "Database Error", 500))
    } yield comments
  }
}