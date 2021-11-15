package com.rhysmills.comment.models

case class RawComment(
  articlePath: String,
  content: String,
  author: String,
  timestamp: Long,
  parentCommentId: Option[String],
)

case class Comment(
  articlePath: String,
  commentId: String,
  content: String,
  author: String,
  timestamp: Long,
  parentCommentId: Option[String],
)

case class Failure(
  logMessage: String,
  friendlyMessage: String,
  statusCode: Int,
)

sealed trait Response extends Product with Serializable
case class EmptyResponse() extends Response
case class CommentsResponse(
  comments: List[Comment]
) extends Response