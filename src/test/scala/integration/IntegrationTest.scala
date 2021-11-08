package integration

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.rhysmills.comment.Comments.{getCommentsForArticleId, myActualProgram}
import com.rhysmills.comment.models._
import org.scalactic.source.Position
import org.scalatest.EitherValues
import org.scalatest.exceptions.TestFailedException
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable


class IntegrationTest extends AnyFreeSpec with Matchers with EitherValues {
  "integration tests" - {
    val testDB = new TestDB(None)
    val lambdaLogger = new LambdaLogger {
      override def log(message: String): Unit = println(message)
      override def log(message: Array[Byte]): Unit = ???
    }
    "Check that POST populates the database" in {
      myActualProgram(Map.empty, lambdaLogger, testDB, "POST", Some(
        """
          |{
          |  "articlePath": "article1",
          |  "commentId": "abc1",
          |  "content": "Here is my comment.",
          |  "author": "Smallie Biggs",
          |  "timestamp": 1635781282,
          |  "parentCommentId": null
          |}
          |""".stripMargin))
      val comments = testDB.getComments("article1")

      comments.value should contain (Comment("article1", "abc1", "Here is my comment.", "Smallie Biggs", 1635781282, None))
    }
    "Check that GET returns the right comments." in {
      val testDB = new TestDB(Some(
          mutable.Map(
              "fun-post-1" -> List(
                Comment("fun-post-1", "abc1", "Here is my comment.", "Smallie Biggs", 1635781282, None),
                Comment("fun-post-1", "abc2", "Here is my second comment.", "Smallie Biggs", 1635781285, None)
              ),
              "fun-post-2" -> List(Comment("fun-post-2", "abc3", "Here is my third comment.", "Smallie Biggs", 1635781289, None))
            )
        )
      )

      val commentsResponse = getCommentsForArticleId(Map("articleId" -> "fun-post-1"), testDB).value

      commentsResponse.comments shouldEqual List(
        Comment("fun-post-1", "abc1", "Here is my comment.", "Smallie Biggs", 1635781282, None),
        Comment("fun-post-1", "abc2", "Here is my second comment.", "Smallie Biggs", 1635781285, None)
      )
    }

  }

  implicit class RichEither[L, R](e: Either[L, R]) {
    def value(implicit pos: Position): R = {
      e.fold(
        { l =>
          throw new TestFailedException(
            _ => Some(s"The Either on which value was invoked was not a Right, got Left($l)"),
            None, pos
          )
        },
        identity
      )
    }
  }
}

