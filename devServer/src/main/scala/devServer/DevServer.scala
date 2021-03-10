package devServer

import com.amazonaws.services.lambda.runtime.{Client, ClientContext, CognitoIdentity, Context, LambdaLogger}
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.rhysmills.comment.Lambda
import io.javalin.Javalin

import java.util
import scala.jdk.CollectionConverters.{CollectionHasAsScala, MapHasAsJava, MapHasAsScala}

object DevServer {
  def main(args: Array[String]): Unit = {
    val app = Javalin.create.start(7000)
    app.get("/", { ctx =>
      val response = new Lambda().handleRequest(
        new FakeAPIGatewayRequest(ctx.method(), ctx.queryParamMap()), new FakeContext()
      )
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
      override def getInstallationId: String = ???
      override def getAppTitle: String = ???
      override def getAppVersionName: String = ???
      override def getAppVersionCode: String = ???
      override def getAppPackageName: String = ???
    }
    override def getCustom: util.Map[String, String] = ???
    override def getEnvironment: util.Map[String, String] = ???
  }

  override def getRemainingTimeInMillis: Int = 1000

  override def getMemoryLimitInMB: Int = 2000

  override def getLogger: LambdaLogger = ???
}