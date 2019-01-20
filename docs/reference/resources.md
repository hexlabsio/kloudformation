---
layout: default
title: Resources
parent: Reference
nav_order: 5
---
<script src="https://unpkg.com/kotlin-playground@1" data-selector=".kotlin"></script>
<style>
blockquote{
    color: #666;
    margin: 0;
    padding-left: 3em;
    border-left: 0.5em #f2c152 solid;
}
</style>

## Table of contents
{: .no_toc .text-delta }

* TOC
{:toc}

# Resources

The Resources section defines the AWS Resources that you wish to create using this template.
All of the AWS Resources that are present in CloudFormation also appear in KloudFormation.

> Info on Resources from AWS can be found  [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/resources-section-structure.html)

Within the curly braces of the `KloudFormation.create()` function you have access to all of the resource types.
To see a list in IntelliJ you can press the hotkey for code completion (Ctrl + Space on Mac).

Each resource has a function of the same name. That function has a receiver of type `KloudFormation` meaning that the function is available inside the `{ }` braces

The following will create an S3 Bucket:

<pre class="kotlin" data-highlight-only>
override fun KloudFormation.create() {
    bucket()
}
</pre>

This produces the following template:

```yaml
Resources:
  Bucket:
    Type: "AWS::S3::Bucket"
```

## Resource Parameters

Each resource takes the following parameters:
 
 * Required Resource Properties
 * `logicalName`
 * `builder`
 * `dependsOn`
 * `resourceProperties`
 
### Required Resource Properties

<pre class="kotlin" data-highlight-only>
class Stack: StackBuilder {
    override fun KloudFormation.create() {
        topic() // No required properties
        vPC(cidrBlock = +"0.0.0.0/0") // cidrBlock is required
    }
}
</pre>
 
### Logical Name

The Logical Name parameter determines what the resource will be named inside your stack. This will appear in the CloudFormation console.

Logical names are allocated for you if you do not set one and will appear as the capitalised name of the Resource followed by a number that increments.

Here is an example with three S3 Buckets:

<pre class="kotlin" data-highlight-only>
bucket()
bucket()
bucket(logicalName = "MyBucket")
</pre>

Produces

```yaml
Bucket:
  Type: "AWS::S3::Bucket"
Bucket2:
  Type: "AWS::S3::Bucket"
MyBucket:
  Type: "AWS::S3::Bucket"
```

### Optional Resource Properties

The builder is always the last parameter and is a function so can be passed as curly braces.

Any Properties that are optional appear as functions within the resource Builder which is passed to that function.

For example `bucketName` is optional so appears within the `{ }` braces

<pre class="kotlin" data-highlight-only>
bucket { 
    bucketName("MyBucket")
}
</pre>

### Resource Dependencies

You can pass a list of logical names to the `dependsOn` parameter so that CloudFormation creates the resources in the correct order.

<pre class="kotlin" data-highlight-only>
val topic = topic()
val q = queue(dependsOn = listOf(topic.logicalName))
</pre>

This Produces

```yaml
Resources:
  Topic:
    Type: "AWS::SNS::Topic"
  Queue:
    Type: "AWS::SQS::Queue"
    DependsOn:
    - "Topic"
```

This can also be achieved by chaining with the `then()` function.

<pre class="kotlin" data-highlight-only>
topic().then{ queue() }
</pre>

Or, for multiple dependencies:

<pre class="kotlin" data-highlight-only>
(topic() and topic()).then{ queue()  }
</pre>

### Resource Properties

Other metadata can be passed to the `resourceProperties` parameter.

ResourceProperties takes the following optional parameters:

* condition
* metadata
* updatePolicy
* creationPolicy
* deletionPolicy
* otherProperties

##### Condition
Condition has its own docs [here](./conditions.html)

##### Metadata

The metadata section allows you to pass JSON. It takes the `Value<JsonNode>` type described [here](./fundamentals.html#the-valuejsonnode-type)

> For information on other Metadata elements like `AWS::CloudFormation::Init`, see [Metadata](./metadata.html)

<pre class="kotlin" data-highlight-only>
topic(resourceProperties = ResourceProperties(
    metadata = json(mapOf(
            "Key1" to "Value1",
            "Key2" to listOf("Value2", "Value3")
    ))
))
</pre>

##### Update Policy

The update policy attribute tells CloudFormation what to do when this resource updates.

<pre class="kotlin" data-highlight-only>
ResourceProperties(
    updatePolicy = UpdatePolicy(
            autoScalingReplacingUpdate = AutoScalingReplacingUpdate(willReplace = +true)
    )
)
</pre>

##### Creation Policy

The creation policy attribute tells CloudFormation what to do when this resource is created.

<pre class="kotlin" data-highlight-only>
ResourceProperties(
        creationPolicy = CreationPolicy(
                resourceSignal = CreationPolicy.ResourceSignal(
                        count = Value.Of(3),
                        timeout = +"PT15M"
                )
        )
)
</pre>

##### Deletion Policy

The deletion policy attribute tells CloudFormation what to do when this resource is deleted.

<pre class="kotlin" data-highlight-only>
ResourceProperties(
        deletionPolicy = DeletionPolicy.RETAIN.policy
)
</pre>

##### Other Properties

If KloudFormation does not have the property you need to set, you can add any extra properties by passing them to the otherProperties attribute.

<pre class="kotlin" data-highlight-only>
topic(resourceProperties = ResourceProperties(
        otherProperties = mapOf("MyProperty" to "Value")
))
</pre>

```yaml
Resources:
  Topic:
    Type: "AWS::SNS::Topic"
    Properties:
      MyProperty: "Value"
```

## Custom Resources

There are two types of custom resources:
 
* `AWS::CloudFormation::CustomResource`
* `Custom::<some string>`

Both types can be made with the `customResource()` function provided. In order to change the type to `Custom::<some string>` invoke the `asCustomResource` function after building. Shown Below:

<pre class="kotlin" data-highlight-only>
val standardCustomResource = customResource(
        logicalName = "DatabaseInitializer",
        serviceToken = +"arn:aws::xxxx:xxx"
).asCustomResource(properties = mapOf(
        "A" to "B",
        "C" to "D"
))
val customNameCustomResource = customResource(
        logicalName = "DatabaseInitializer2",
        serviceToken = +"arn:aws::xxxx:xxx"
).asCustomResource("Custom::DBInit")
</pre>

```yaml
Resources:
  DatabaseInitializer:
    Type: "AWS::CloudFormation::CustomResource"
    Properties:
      ServiceToken: "arn:aws::xxxx:xxx"
      A: "B"
      C: "D"
  DatabaseInitializer2:
    Type: "Custom::DBInit"
    Properties:
      ServiceToken: "arn:aws::xxxx:xxx"
```
