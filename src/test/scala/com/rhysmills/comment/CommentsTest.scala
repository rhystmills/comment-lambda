package com.rhysmills.comment

import com.rhysmills.comment.Comments.validateComment
import com.rhysmills.comment.models.Comment
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

class CommentsTest extends AnyFreeSpec with Matchers {
  "validateComment" - {
    val validComment = Comment("article1", "abc1", "Here is my comment.", "Smallie Biggs", 0, None)
    "Comment body equal to or less than 10000 characters is valid" in {
      validateComment(validComment) shouldEqual Right(validComment)
    }
    "Comment body over 10000 characters is invalid" in {
      validateComment(validComment.copy(content = "a" * 10001)).isLeft shouldEqual true
    }
    "Comment body with 0 characters is invalid" in {
      validateComment(validComment.copy(content = "")).isLeft shouldEqual true
    }

  }
}
