package com.rhysmills.comment.models

case class Comment(
  articlePath: String,
  id: String,
  content: String,
  author: String,
  timestamp: Long,
  parentId: Option[String],
)

case class Failure(
  logMessage: String,
  friendlyMessage: String,
  statusCode: Int,
)