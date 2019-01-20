# Kloud Formation
[![CircleCI](https://circleci.com/gh/hexlabsio/kloudformation/tree/master.svg?style=svg)](https://circleci.com/gh/hexlabsio/kloudformation/tree/master)
[ ![Download](https://api.bintray.com/packages/hexlabsio/kloudformation/kloudformation/images/download.svg) ](https://bintray.com/hexlabsio/kloudformation/kloudformation)

![KloudFormation](kloud-formation-logo-white.png)

> For Full Reference Documentation goto: https://hexlabsio.github.io/kloudformation/

# KloudFormation

KloudFormation is a one-to-one mapping of Amazon's CloudFormation generated into a Kotlin library allowing users to write type safe stacks in Kotlin.

KloudFormation can be invoked in many ways allowing any project to use it, not just Kotlin projects.

**Benefits Include**

 * Type Safety
 * Code Completion
 * Stacks as Code
 * Modular Templating
 * Up to Date

## This is what it looks like

```kotlin
val template = KloudFormationTemplate.create {
    val topic = topic()
    bucket {
        bucketName(topic.TopicName())
    }
}
```

This is what it Produces

```yaml
AWSTemplateFormatVersion: "2010-09-09"
Resources:
    Topic:
     Type: "AWS::SNS::Topic"
    Bucket:
     Type: "AWS::S3::Bucket"
     Properties:
       BucketName:
         Fn::GetAtt:
         - "Topic"
         - "TopicName"
```

## Get Started

Any project can use KloudFormation. To get the best experience, it is recommended to use an IDE with code completion like IntelliJ IDEA

Get Started with [Gradle](https://hexlabsio.github.io/kloudformation/get-started/gradle.html), [Maven](https://hexlabsio.github.io/kloudformation/get-started/maven.html), or [No Build Tool](https://hexlabsio.github.io/kloudformation/get-started/other.html)
