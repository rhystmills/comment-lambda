package devServer

import com.amazonaws.services.lambda.runtime._
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.rhysmills.comment.models.Comment
import com.rhysmills.comment.{Comments, Lambda}
import io.javalin.Javalin
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
  println("howdy")

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
      val queryMap = ctx.queryParamMap().asScala.toMap.map { case (key, values) =>
        (key, values.asScala.head)
      }
      val response = Comments.myActualProgram(queryMap, new FakeContext(), dbClient, tableName)
      ctx.result(response.getBody)
    })

    Runtime.getRuntime.addShutdownHook(new Thread(() => {
      println("[INFO] Stopping...")
      app.stop()
    }))
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

class FakeContext extends Context {
  override def getAwsRequestId: String = "id"

  override def getLogGroupName: String = "logGroup"

  override def getLogStreamName: String = "logStream"

  override def getFunctionName: String = "walter"

  override def getFunctionVersion: String = "0.0.1"

  override def getInvokedFunctionArn: String = "arn"

  override def getIdentity: CognitoIdentity = new CognitoIdentity {
    override def getIdentityId: String = "123"
    override def getIdentityPoolId: String = "321"
  }

  override def getClientContext: ClientContext = new ClientContext {
    override def getClient: Client = new Client {
      override def getInstallationId: String = "???"
      override def getAppTitle: String = "???"
      override def getAppVersionName: String = "???"
      override def getAppVersionCode: String = "???"
      override def getAppPackageName: String = "???"
    }
    override def getCustom: util.Map[String, String] = ???
    override def getEnvironment: util.Map[String, String] = ???
  }

  override def getRemainingTimeInMillis: Int = 1000

  override def getMemoryLimitInMB: Int = 2000

  override def getLogger: LambdaLogger = new LambdaLogger {
    override def log(message: String): Unit = println(message)

    override def log(message: Array[Byte]): Unit = ???
  }
}