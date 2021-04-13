import * as cdk from '@aws-cdk/core';
import * as apigateway from '@aws-cdk/aws-apigateway';
import * as lambda from '@aws-cdk/aws-lambda';
import * as dynamoDB from '@aws-cdk/aws-dynamodb';
import {AttributeType} from '@aws-cdk/aws-dynamodb';

import * as path from 'path';

export class CdkStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);
    // Read from ssm:
    // const myKey = ssm.StringParameter
    //   .valueForStringParameter(this, '/myprofilename/app/my-key', 1);

    const table = new dynamoDB.Table(this, "comment-lambda-table", {
      billingMode: dynamoDB.BillingMode.PAY_PER_REQUEST,
      partitionKey: { name: "articlePath", type: AttributeType.STRING },
      sortKey: { name: "commentId", type: AttributeType.STRING },
    });

    const backend = new lambda.Function(this, "comment-lambda", {
      runtime: lambda.Runtime.JAVA_8,
      timeout: cdk.Duration.seconds(10),
      memorySize: 1024,
      handler: 'com.rhysmills.comment.Lambda::handleRequest',
      code: lambda.Code.fromAsset(path.join(__dirname, '../../target/universal/comment-lambda.zip')),
      environment: {
        region: this.region,
        tableName: table.tableName,
      }
    });

    table.grantReadWriteData(backend);

    new apigateway.LambdaRestApi(this, 'myapi', {
      handler: backend,
    });
  }
}
