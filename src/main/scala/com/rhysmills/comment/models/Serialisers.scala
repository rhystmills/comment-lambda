package com.rhysmills.comment.models

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object Serialisers {
  implicit val commentEncoder: Encoder[Comment] = deriveEncoder
  implicit val commentsResponseEncoder: Encoder[CommentsResponse] = deriveEncoder
  implicit val emptyResponseEncoder: Encoder[EmptyResponse] = deriveEncoder
  implicit val responseEncoder: Encoder[Response] = Encoder.instance {
    case emptyResponse: EmptyResponse => emptyResponseEncoder(emptyResponse)
    case commentsResponse: CommentsResponse => commentsResponseEncoder(commentsResponse)
  }
  implicit val commentDecoder: Decoder[Comment] = deriveDecoder
}
