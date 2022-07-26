import * as cdk from '@aws-cdk/core';
import * as apigateway from '@aws-cdk/aws-apigateway';
import * as lambda from '@aws-cdk/aws-lambda';
import * as dynamoDB from '@aws-cdk/aws-dynamodb';
import {AttributeType} from '@aws-cdk/aws-dynamodb';
import * as cognito from '@aws-cdk/aws-cognito';
import * as ssm from '@aws-cdk/aws-ssm';


import * as path from 'path';
// declare const providerAttribute: cognito.ProviderAttribute;

export class CdkStack extends cdk.Stack {
  constructor(scope: cdk.Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);
    // Read from ssm:
    const googleAuthClientId = ssm.StringParameter
        .valueForStringParameter(this, '/rhysmills.com/google-auth-client-id', 1);
    const googleAuthClientSecret = ssm.StringParameter
        .valueForStringParameter(this, '/rhysmills.com/google-auth-client-secret', 1);

    const userPoolIdentityProviderGoogle = new cognito.UserPoolIdentityProviderGoogle(this, 'MyUserPoolIdentityProviderGoogle', {
      clientId: googleAuthClientId,
      clientSecret: googleAuthClientSecret,
      userPool: new cognito.UserPool(this, "comment-lambda-user-pool"),
    });

    const table = new dynamoDB.Table(this, "comment-lambda-table", {
      billingMode: dynamoDB.BillingMode.PAY_PER_REQUEST,
      partitionKey: { name: "articlePath", type: AttributeType.STRING },
      sortKey: { name: "commentId", type: AttributeType.STRING },
    });


    const backend = new lambda.Function(this, "comment-lambda", {
      runtime: lambda.Runtime.JAVA_8, // @ts-ignore
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

// The code below shows an example of how to instantiate this type.
// The values are placeholders you should change.
// import * as cognito from '@aws-cdk/aws-cognito';
//
// declare const providerAttribute: cognito.ProviderAttribute;
// declare const userPool: cognito.UserPool;

// const userPoolIdentityProviderGoogle = new cognito.UserPoolIdentityProviderGoogle(this, 'MyUserPoolIdentityProviderGoogle', {
//   clientId: 'clientId',
//   clientSecret: 'clientSecret',
//   userPool: userPool,
//
//   // the properties below are optional
//   attributeMapping: {
//     address: providerAttribute,
//     birthdate: providerAttribute,
//     custom: {
//       customKey: providerAttribute,
//     },
//     email: providerAttribute,
//     familyName: providerAttribute,
//     fullname: providerAttribute,
//     gender: providerAttribute,
//     givenName: providerAttribute,
//     lastUpdateTime: providerAttribute,
//     locale: providerAttribute,
//     middleName: providerAttribute,
//     nickname: providerAttribute,
//     phoneNumber: providerAttribute,
//     preferredUsername: providerAttribute,
//     profilePage: providerAttribute,
//     profilePicture: providerAttribute,
//     timezone: providerAttribute,
//     website: providerAttribute,
//   },
//   scopes: ['scopes'],
// });
