package com.rhysmills.comment

import org.scalactic.source.Position
import org.scalatest.exceptions.TestFailedException
import org.scalatest.matchers.HavePropertyMatcher
import org.scalatest.matchers.should.Matchers


trait TestHelpers extends Matchers {
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

  implicit class HavingTestHelperString(propertyName: String) {
    def as[A](propertyValue: A)(implicit pos: Position): HavePropertyMatcher[AnyRef, Any] = {
      Symbol(propertyName) (propertyValue)
    }
  }
}
