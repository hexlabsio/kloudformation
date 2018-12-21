# Kloud Formation
[![CircleCI](https://circleci.com/gh/hexlabsio/kloudformation/tree/master.svg?style=svg)](https://circleci.com/gh/hexlabsio/kloudformation/tree/master)
[ ![Download](https://api.bintray.com/packages/hexlabsio/kloudformation/kloudformation/images/download.svg?version=0.1.12) ](https://bintray.com/hexlabsio/kloudformation/kloudformation/0.1.12/link)

![KloudFormation](kloud-formation-logo-white.png)

# Get Started

KloudFormation can be run a couple of different ways:

> An Example Maven project can be found [here](https://github.com/hexlabsio/kloudformation-specification-sandbox).

1. As a Jar with `java -jar kloudformation.jar io.kloudformation.StackBuilderKt <name of StackBuilder Class> <name of output file>`
2. In your project as a test dependency

## Maven
1. Create a directory under the `src` folder named `stack`
2. Add a new file under the `stack` folder named `kloudformation.kt`
3. Create a class named `Stack` that implements `io.kloudformation.StackBuilder`
4. Implement the function named `KloudFormation.create()` 
5. Add the following to the pom.xml:

### Dependency
```xml
<dependency>
    <groupId>io.kloudformation</groupId>
    <artifactId>kloudformation</artifactId>
    <version>${kloudformation.version}</version>
    <scope>test</scope>
</dependency>
```

### Plugins
Depending on your project the kotlin maven plugin may look different but the key change is that the folder including the stack code is listed under the test-compile sources as shown below.

If you are using java only you can omit the compile execution.
```xml
<plugin>
    <artifactId>kotlin-maven-plugin</artifactId>
    <groupId>org.jetbrains.kotlin</groupId>
    <version>${kotlin.version}</version>
    <executions>
        <execution>
            <id>compile</id>
            <goals>
                <goal>compile</goal>
            </goals>
            <configuration>
                <sourceDirs>
                    <sourceDir>${project.basedir}/src/main/kotlin</sourceDir>
                </sourceDirs>
            </configuration>
        </execution>
        <execution>
            <id>test-compile</id>
            <goals>
                <goal>test-compile</goal>
            </goals>
            <configuration>
                <sourceDirs>
                    <sourceDir>${project.basedir}/src/test/kotlin</sourceDir>
                    <sourceDir>${project.basedir}/src/stack</sourceDir>
                </sourceDirs>
            </configuration>
        </execution>
    </executions>
</plugin>
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>exec-maven-plugin</artifactId>
    <version>1.4.0</version>
    <executions>
        <execution>
            <phase>process-test-classes</phase>
            <goals>
                <goal>java</goal>
            </goals>
            <configuration>
                <mainClass>io.kloudformation.StackBuilderKt</mainClass>
                <arguments>
                    <argument>Stack</argument>
                    <argument>${project.basedir}/template.yml</argument>
                </arguments>
                <classpathScope>test</classpathScope>
            </configuration>
        </execution>
    </executions>
</plugin>
```

# Inversion - (Template to Code)
Any cloudformation template can be translated to kloudformation kotlin code using this project.
 1. Download the jar from maven central or checkout the code and run mvn package.
 2. Open a terminal
 3. Run `java -jar kloudformation.jar <location of template> <directory to generate code>`
 

# Building Templates in Kotlin

## Create a new  Template

```kotlin
val template = KloudFormationTemplate.create {}
```

## Parameters

```kotlin
val template = KloudFormationTemplate.create {
    val email = parameter<String>("EmailAddress")
}
// Result:
// AWSTemplateFormatVersion: "2010-09-09"
// Parameters:
//   EmailAddress:
//     Type: "String"
```

## Pseudo Parameters

Pseudo parameters can be used anywhere a reference can be.

They exist within the `KloudFormationTemplate.Builder` type and are available within the `create` braces

Here is the full list of available pseudo parameters:

```kotlin
val awsAccountId = Reference<String>("AWS::AccountId")
val awsNotificationArns = Reference<List<String>>("AWS::NotificationARNs")
fun <T> awsNoValue() = Reference<T>("AWS::NoValue")
val awsPartition = Reference<String>("AWS::Partition")
val awsRegion = Reference<String>("AWS::Region")
val awsStackId = Reference<String>("AWS::StackId")
val awsStackName = Reference<String>("AWS::StackName")
val awsUrlSuffix = Reference<String>("AWS::URLSuffix")
```

## Resources

```kotlin
val template = KloudFormationTemplate.create {
    topic()
}
// Result:
// AWSTemplateFormatVersion: "2010-09-09"
// Resources:
//   Topic:
//     Type: "AWS::SNS::Topic"
```

## References

To get a reference to any resource or parameter simply invoke the `ref()` function

```kotlin
val template = KloudFormationTemplate.create {
    val email = parameter<String>("EmailAddress")
    val topic = topic()
    subscription {
        topicArn(topic.ref())
        endpoint(email.ref())
        protocol("email")
    }
}
// Result:
// AWSTemplateFormatVersion: "2010-09-09"
// Parameters:
//   EmailAddress:
//     Type: "String"
// Resources:
//   Topic:
//     Type: "AWS::SNS::Topic"
//   Subscription:
//     Type: "AWS::SNS::Subscription"
//     Properties:
//       Endpoint:
//         Ref: "EmailAddress"
//       Protocol: "email"
//       TopicArn:
//         Ref: "Topic"
```

## Complex Resources
Required properties appear in the resources building function itself, like cidrBlock in the following example. 
Other properties can be set in the builder function passed to resource builder and are all exposed as functions themselves. (see enableDnsHostnames)

```kotlin
 val template = KloudFormationTemplate.create{
    vPC(cidrBlock = +"0.0.0.0/0"){
        enableDnsHostnames(true)
    }
}
// Result:
// AWSTemplateFormatVersion: "2010-09-09"
// Resources:
//   VPC:
//     Type: "AWS::EC2::VPC"
//     Properties:
//       CidrBlock: "0.0.0.0/0"
//       EnableDnsHostnames: true
```


## Attributes

Each class representing an AWS Resource contains a list of available attributes.
Attributes can be used anywhere a reference can be used or a primitive type.
The following example shows how a bucket could be named with the name of a topic

```kotlin
val template = KloudFormationTemplate.create{
    val topic = topic()
    bucket { 
        bucketName(topic.TopicName())
    }
}
// Result:
// AWSTemplateFormatVersion: "2010-09-09"
// Description: ""
// Resources:
//   Topic:
//     Type: "AWS::SNS::Topic"
//   Bucket:
//     Type: "AWS::S3::Bucket"
//     Properties:
//       BucketName:
//         Fn::GetAtt:
//         - "Topic"
//         - "TopicName"
```

## Joins

Where primitive types, like String are required the actual value can either be the primitive type itself, a reference, an attribute or a join.
Each type can be combined using the plus operator to create a join. The following example is the same as the above example except it adds the string `Bucket` to the value returned from getting the topics name at runtime.

```kotlin
val template = KloudFormationTemplate.create{
    val topic = topic()
    bucket { 
        bucketName(topic.TopicName() + "Bucket")
    }
}
// Result:
// AWSTemplateFormatVersion: "2010-09-09"
// Resources:
//   Topic:
//     Type: "AWS::SNS::Topic"
//   Bucket:
//     Type: "AWS::S3::Bucket"
//     Properties:
//       BucketName:
//         Fn::Join:
//         - ""
//         - - Fn::GetAtt:
//             - "Topic"
//             - "TopicName"
//           - "Bucket"
```

## JSON
Some sections in a cloudformation template take raw JSON. Anywhere that takes JSON you can use the `json` function to pass a map. 

Alternatively, if a policy document is needed there are specially designed objects to build those up, shown below.

## IAM Policy Documents

AWS simply state that certain resources are of the type JsonNode. To remedy this we have wrapped that in our Value type so that our PolicyDocument type can be used instead.
PolicyDocument lets you build fully type safe iam policies as shown in the example below. 

```kotlin
  val template = KloudFormationTemplate.create{
     val topic = topic(logicalName = "NotificationTopic") // Provide custom logical names
     role(
             assumeRolePolicyDocument = policyDocument {
                 statement(action("sts:AssumeRole")) {
                     principal(PrincipalType.SERVICE, +"lambda.amazonaws.com")
                 }
             }
     ){
         policies(listOf(
                 Policy(
                         policyDocument {
                             statement(
                                     action("sns:*"),
                                     IamPolicyEffect.Allow,
                                     resource(topic.ref())
                             ) {
                                 principal(PrincipalType.AWS, +"Some AWS Principal")
                                 condition(ConditionOperators.stringEquals, ConditionKeys.awsUserName, listOf("brian"))
                             }
                         },
                         policyName = +"LambdaSnsPolicy"
                 )
         ))
     }
 }
// Result:
// AWSTemplateFormatVersion: "2010-09-09"
// Resources:
//   NotificationTopic:
//     Type: "AWS::SNS::Topic"
//   Role:
//     Type: "AWS::IAM::Role"
//     Properties:
//       AssumeRolePolicyDocument:
//         Statement:
//         - Effect: "Allow"
//           Action:
//           - "sts:AssumeRole"
//           Principal:
//             Service: "lambda.amazonaws.com"
//       Policies:
//       - PolicyDocument:
//           Statement:
//           - Effect: "Allow"
//             Action:
//             - "sns:*"
//             Resource:
//             - Ref: "NotificationTopic"
//             Principal:
//               AWS: "Some AWS Principal"
//             Condition:
//               StringEquals:
//                 aws:username:
//                 - "brian"
//         PolicyName: "LambdaSnsPolicy"
```

## Alternative Resource References

```kotlin
val template = KloudFormationTemplate.create {
    topic().also { myTopic ->
        subscription {
            topicArn(myTopic.ref())
        }
    }
}
```

## Dependencies

Each builder function takes a `dependsOn` parameter that can be used to set a dependency as follows

```kotlin
val topic = topic()
val q = queue(dependsOn = listOf(topic.logicalName))
```

Alternatively you can chain resources using the `then` function as follows

```kotlin
topic().then{ queue() }
```

You can also combine resources for multi dependant relationships as follows

```kotlin
 (topic() and topic()).then{ topic()  }
 
 // Result
 // AWSTemplateFormatVersion: "2010-09-09"
 // Resources:
 //   Topic:
 //     Type: "AWS::SNS::Topic"
 //   Topic2:
 //     Type: "AWS::SNS::Topic"
 //   Topic3:
 //     Type: "AWS::SNS::Topic"
 //     DependsOn:
 //     - "Topic"
 //     - "Topic2"
```


## Mappings

There is a function exposed when inside the `create` braces called `mappings` that allows you to create the mappings section within your template.

It takes a three level nested map. The following example shows how you might create an ec2 instance based on the region your stack is being created in.

_Note here that the `awsRegion` is one of the Pseudo Parameters (see above for full list)_

```kotlin
val template = KloudFormationTemplate.create {
    val regions = "RegionMap"
    val usEast1 = "us-east-1"
    val euWest1 = "eu-west-1"
    val bits32 = "32"
    mappings(
        regions to mapOf(
                usEast1 to mapOf( bits32 to +"ami-6411e20d"),
                euWest1 to mapOf( bits32 to +"ami-37c2f643")
        )
    )
    instance{
        instanceType("m1.small")
        imageId(FindInMap(+regions, awsRegion, +bits32))
    }
}

// Result
// AWSTemplateFormatVersion: "2010-09-09"
// Mappings:
//   RegionMap:
//     us-east-1:
//       32: "ami-6411e20d"
//     eu-west-1:
//       32: "ami-37c2f643"
// Resources:
//   Instance:
//     Type: "AWS::EC2::Instance"
//     Properties:
//       ImageId:
//         Fn::FindInMap:
//         - "RegionMap"
//         - Ref: "AWS::Region"
//         - "32"
//       InstanceType: "m1.small"
```

## Conditions
There is a function exposed when inside the `create` braces called `conditions` that allows you to create the conditions section within your template.

Here is an example of how to create resources only in production based on an environment variable that is passed in.

```kotlin
val template = KloudFormationTemplate.create {
    val environment = parameter<String>(
            logicalName = "Environment",
            description = "AWS Environmnet",
            default = "nonprod",
            allowedValues = listOf("nonprod", "prod")
    )
    val inProduction = "InProduction"
    conditions(
            inProduction to (environment.ref() eq +"prod")
    )
    instance(condition = inProduction){
        instanceType("m1.small")
    }
}

// Result
// AWSTemplateFormatVersion: "2010-09-09"
// Parameters:
//   Environment:
//     Type: "String"
//     AllowedValues:
//     - "nonprod"
//     - "prod"
//     Default: "nonprod"
//     Description: "AWS Environmnet"
// Conditions:
//   InProduction:
//     Fn::Equals:
//     - Ref: "Environment"
//     - "prod"
// Resources:
//   Instance:
//     Type: "AWS::EC2::Instance"
//     Condition: "InProduction"
//     Properties:
//       InstanceType: "m1.small"
```

## Conditional Logic
Within the conditions section shown above within a template you will have full access to any of the conditional functions shown below.

### Equals
```kotlin
val envIsProd = environment.ref() eq +"prod"
```

### Or
```kotlin
val envIsDevOrTest = (environment.ref() eq +"dev") or (environment.ref() eq +"test")
```

### And
```kotlin
val envIsProdAndHuge = (environment.ref() eq +"prod") and (size.ref() eq +"huge")
```

### Not
```kotlin
val envIsNotProd = not(environment.ref() eq +"prod")
```

## If Function
Once a Condition is set up you can use the if function to pick a value based on the condition being true or false

In the following example an ec2 instance will be created. If in prod it will be an m1.large but in dev it will be a t2.micro

```kotlin
val template = KloudFormationTemplate.create {
    val environment = parameter<String>(
            logicalName = "Environment",
            description = "AWS Environmnet",
            default = "nonprod",
            allowedValues = listOf("nonprod", "prod")
    )
    val inProduction = "InProduction"
    conditions(
            inProduction to (environment.ref() eq +"prod")
    )
    instance{
        instanceType(If(inProduction, +"m1.large", +"t2.micro"))
    }
}
// Result
// AWSTemplateFormatVersion: "2010-09-09"
// Parameters:
//   Environment:
//     Type: "String"
//     AllowedValues:
//     - "nonprod"
//     - "prod"
//     Default: "nonprod"
//     Description: "AWS Environmnet"
// Conditions:
//   InProduction:
//     Fn::Equals:
//     - Ref: "Environment"
//     - "prod"
// Resources:
//   Instance:
//     Type: "AWS::EC2::Instance"
//     Properties:
//       InstanceType:
//         Fn::If:
//         - "InProduction"
//         - "m1.large"
//         - "t2.micro"
```

## Custom Resources
There are two types of custom resources: `AWS::CloudFormation::CustomResource` and `Custom::<some string>`.

Both types can be made with the customResource function provided. In order to change the type to `Custom::<some string>` call a method named `asCustomResource` after building.
Shown Below:

```kotlin
val template = KloudFormationTemplate.create {
    val topic = topic()
    val standardCustomResource = customResource(
            logicalName = "DatabaseInitializer",
            serviceToken = +"arn:aws::xxxx:xxx",
            metadata = json(mapOf(
                    "SomeKey" to "SomeValue"
            ))
    ).asCustomResource(properties = mapOf(
        "A" to "B",
        "C" to topic.ref()
    ))
    val customNameCustomResource = customResource(
            logicalName = "DatabaseInitializer2",
            serviceToken = +"arn:aws::xxxx:xxx",
            metadata = json(mapOf(
                    "SomeKey" to "SomeValue"
            ))
    ).asCustomResource("Custom::DBInit")
}

// Result
// AWSTemplateFormatVersion: "2010-09-09"
// Resources:
//   Topic:
//     Type: "AWS::SNS::Topic"
//   DatabaseInitializer:
//     Type: "AWS::CloudFormation::CustomResource"
//     Metadata:
//       SomeKey: "SomeValue"
//     Properties:
//       ServiceToken: "arn:aws::xxxx:xxx"
//       A: "B"
//       C:
//         Ref: "Topic"
//   DatabaseInitializer2:
//     Type: "Custom::DBInit"
//     Metadata:
//       SomeKey: "SomeValue"
//     Properties:
//       ServiceToken: "arn:aws::xxxx:xxx"
```

## Lifecycle Policies

### Creation Policy
```kotlin
val template = KloudFormationTemplate.create {
    autoScalingGroup(minSize = +"1", maxSize = +"4"){
        availabilityZones(GetAZs(+""))
    }
}

// Result
// AWSTemplateFormatVersion: "2010-09-09"
// Resources:
//   AutoScalingGroup:
//     Type: "AWS::AutoScaling::AutoScalingGroup"
//     Properties:
//       MaxSize: "4"
//        MinSize: "1"
//       AvailabilityZones:
//         Fn::GetAZs:
//         - ""
```

### Update Policy
```kotlin
val template = KloudFormationTemplate.create {

    alias(
            functionName = lambda.ref(),
            functionVersion = lambdaVersion2.Version(),
            name = +"MyAlias",
            updatePolicy = UpdatePolicy(
                    codeDeployLambdaAliasUpdate = CodeDeployLambdaAliasUpdate(
                            applicationName = application.ref(),
                            deploymentGroupName = deploymentGroup.ref()
                    )
            )
    )
}

// Result
//  Alias:
//    Type: "AWS::Lambda::Alias"
//    UpdatePolicy:
//      CodeDeployLambdaAliasUpdate:
//        ApplicationName:
//          Ref: "Topic"
//        DeploymentGroupName:
//          Ref: "Topic3"
//    Properties:
//      FunctionName:
//        Ref: "Topic2"
//      FunctionVersion:
//        Fn::GetAtt:
//        - "Version"
//        - "Version"
//      Name: "MyAlias"
```

### Deletion Policy
```kotlin
val template = KloudFormationTemplate.create {
    bucket(deletionPolicy = DeletionPolicy.RETAIN.policy)
}

// Result
// AWSTemplateFormatVersion: "2010-09-09"
// Resources:
//   Bucket:
//     Type: "AWS::S3::Bucket"
//     DeletionPolicy: "Retain"
```
