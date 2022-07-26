package integration

import com.amazonaws.services.lambda.runtime.LambdaLogger
import com.rhysmills.comment.Comments.{getCommentsForArticlePath, myActualProgram}
import com.rhysmills.comment.TestHelpers
import com.rhysmills.comment.models._
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable


class IntegrationTest extends AnyFreeSpec with Matchers with OptionValues with TestHelpers {
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
          |  "content": "Here is my comment.",
          |  "author": "Smallie Biggs",
          |  "timestamp": 1635781282,
          |  "parentCommentId": null
          |}
          |""".stripMargin))
      val comments = testDB.getComments("article1")

      comments.value.headOption.value should have (
        "articlePath" as "article1",
        "content" as "Here is my comment.",
        "author" as "Smallie Biggs",
        "timestamp" as 1635781282,
        "parentCommentId" as None
      )
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

      val commentsResponse = getCommentsForArticlePath(Map("articlePath" -> "fun-post-1"), testDB).value

      commentsResponse.comments shouldEqual List(
        Comment("fun-post-1", "abc1", "Here is my comment.", "Smallie Biggs", 1635781282, None),
        Comment("fun-post-1", "abc2", "Here is my second comment.", "Smallie Biggs", 1635781285, None)
      )
    }

  }
}

