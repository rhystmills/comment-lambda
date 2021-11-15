package integration

import com.rhysmills.comment.DB
import com.rhysmills.comment.models.{Comment, Failure}
import scala.collection.mutable.{Map => MutableMap}

class TestDB (comments: Option[MutableMap[String, List[Comment]]]) extends DB {
  private val database = comments match {
    case Some(a) => a
    case None => MutableMap.empty[String, List[Comment]]
  }

  override def addComment(comment: Comment): Either[Failure, Unit] = {
    val articlePath = comment.articlePath
    val comments = database.getOrElse(articlePath, Nil)
    val updatedComments = comment :: comments
    database.put(articlePath, updatedComments).toRight(Failure("Failed to add comment", "Failed to add comment", 500)).map(_ => ())
  }

  override def getComments(articlePath: String): Either[Failure, List[Comment]] = {
    database.get(articlePath).toRight(Failure(s"Article $articlePath not found", "No comments found for this article", 404))
  }
}
