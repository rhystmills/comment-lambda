package com.rhysmills.comment.models

import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

object Serialisers {
  implicit val commentEncoder: Encoder[Comment] = deriveEncoder
}
