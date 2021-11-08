package devServer

import com.amazonaws.services.lambda.runtime._
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.rhysmills.comment.models.Comment
import com.rhysmills.comment.{Comments, Lambda, ProdDB}
import io.javalin.Javalin
import io.javalin.http.Context
import org.scanamo.{LocalDynamoDB, Scanamo, ScanamoAsync, Table}
import org.scanamo.syntax._
import org.scanamo.generic.auto._
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType._

import java.util
import scala.concurrent.ExecutionContext.Implicits.global
import scala.jdk.CollectionConverters.{CollectionHasAsScala, MapHasAsJava, MapHasAsScala}

object DevServer {
  val dbClient = LocalDynamoDB.syncClient(8000)
  val tableName = "localTable"
  LocalDynamoDB.createTable(dbClient)(tableName)("articlePath" -> S)
  val commentsTable = Table[Comment](tableName)
  val devDB = new ProdDB(dbClient, tableName)

  Scanamo(dbClient).exec {
//    val newTmp = commentsTable.put(Comment(???)).flatMap { _ =>
//      commentsTable.put(Comment(???)).flatMap { _ =>
//        commentsTable.scan()
//      }
//    }
    // Above - attempt to flatmap version of the for comp
    for {
      _ <- commentsTable.put(Comment("article1", "abc1", "Here is my comment.", "Smallie Biggs", 0, None))
      results <- commentsTable.scan()
    } yield results
  }

  def main(args: Array[String]): Unit = {
    val app = Javalin.create.start(7000)
    app.get("/", { ctx =>
      handler(ctx, "GET")
    })

    app.post("/", { ctx =>
      handler(ctx, "POST")
    })

    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      println("[INFO] Stopping...")
      app.stop()
    }))
  }

  def handler(ctx: Context, httpMethod: String): Context = {
    val queryMap = ctx.queryParamMap().asScala.toMap.map { case (key, values) =>
      (key, values.asScala.head)
    }

    val response = Comments.myActualProgram(queryMap, new PrintLnLambdaLogger(), devDB, httpMethod, Option(ctx.body))
    ctx.result(response.getBody)
  }
}

class FakeAPIGatewayRequest(httpMethod: String, queryString: java.util.Map[String, util.List[String]]) extends APIGatewayProxyRequestEvent {
  override def getHttpMethod(): String = httpMethod

  override def getQueryStringParameters: java.util.Map[String, String] = {
    queryString.asScala.map { case (key, values) =>
      (key, values.asScala.head)
    }.asJava
  }
}

class PrintLnLambdaLogger extends LambdaLogger {
  override def log(message: String): Unit = println(message)

  override def log(message: Array[Byte]): Unit = ???
}