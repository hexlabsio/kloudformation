---
layout: default
title: Parameters
parent: Reference
nav_order: 1
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

# Parameters

The Parameters section in CloudFormation often comes first in a template.

> For more info see the AWS definition [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/parameters-section-structure.html)

A Parameter defines an input variable for the template.

Here is an example of a parameter in CloudFormation

```yaml
Parameters: 
  InstanceTypeParameter: 
    Type: String
    Default: t2.micro
    AllowedValues: 
      - t2.micro
      - m1.small
      - m1.large
    Description: Enter t2.micro, m1.small, or m1.large. Default is t2.micro.
```

This is what it looks like in KloudFormation

<pre class="kotlin" data-highlight-only>
override fun KloudFormation.create() {
    val instanceTypeParameter = parameter&lt;String&gt;(
            logicalName = "InstanceTypeParameter",
            default = "t2.micro",
            allowedValues = listOf("t2.micro", "m1.small", "m1.large"),
            description = "Enter t2.micro, m1.small, or m1.large. Default is t2.micro."
    )
}
</pre>

### Referencing a Parameter within a Template

In CloudFormation you use the Ref Intrinsic function like this

```yaml
Ec2Instance:
  Type: AWS::EC2::Instance
  Properties:
    InstanceType:
      Ref: InstanceTypeParameter
    ImageId: ami-0ff8a91507f77f867
```

In KloudFormation you can invoke the `ref()` function on the parameter.

> ref() returns the `Reference<T>` type which is a `Value<T>`

<pre class="kotlin" data-highlight-only>
instance("Ec2Instance") { 
    instanceType(instanceTypeParameter.ref())
    imageId("ami-0ff8a91507f77f867")
}
</pre>

## Parameter Types
The type property of the parameter takes an instance of `ParameterType` in the `io.kloudformation.model` package.

All the AWS Specific Parameter types are provided under the `ParameterType` class.

<pre class="kotlin" data-highlight-only>
val keyName = parameter(
        logicalName = "KeyName",
        description = "Name of an existing EC2 KeyPair to enable SSH access to the instance",
        type = ParameterType.KeyPairNameParameter,
        constraintDescription = "must be the name of an existing EC2 KeyPair."
)
val securityGroupIds = parameter(
        logicalName = "SecurityGroupIds",
        type = ParameterType.SecurityGroupIdListParameter,
        description = "Security groups that can be used to access the EC2 instances",
        constraintDescription = "must be list of EC2 security group ids"
)
val instanceType = parameter&lt;String&gt;(
        logicalName = "InstanceTypeParameter",
        default = "t2.micro",
        allowedValues = listOf("t2.micro", "m1.small", "m1.large"),
        description = "Enter t2.micro, m1.small, or m1.large. Default is t2.micro."
)
instance(logicalName = "EC2Instance") {
    instanceType(instanceType.ref())
    imageId("ami-0ff8a91507f77f867")
    securityGroupIds(securityGroupIds.ref())
    keyName(keyName.ref())
}
</pre>

Produces the following CloudFormation template

```yaml
Parameters:
  KeyName:
    Type: "AWS::EC2::KeyPair::KeyName"
    ConstraintDescription: "must be the name of an existing EC2 KeyPair."
    Description: "Name of an existing EC2 KeyPair to enable SSH access to the instance"
  SubnetIds:
    Type: "List<AWS::EC2::SecurityGroup::Id>"
    ConstraintDescription: "must be list of EC2 security group ids"
    Description: "Security groups that can be used to access the EC2 instances"
  InstanceTypeParameter:
    Type: "String"
    AllowedValues:
    - "t2.micro"
    - "m1.small"
    - "m1.large"
    Default: "t2.micro"
    Description: "Enter t2.micro, m1.small, or m1.large. Default is t2.micro."
Resources:
  EC2Instance:
    Type: "AWS::EC2::Instance"
    Properties:
      ImageId: "ami-0ff8a91507f77f867"
      InstanceType:
        Ref: "InstanceTypeParameter"
      KeyName:
        Ref: "KeyName"
      SecurityGroupIds:
        Ref: "SubnetIds"
```

## Pseudo Parameters

All of the AWS Pseudo parameters can be found in `io.kloudformation.KloudFormation`

For example, in order to output the account id do the following

<pre class="kotlin" data-highlight-only>
outputs("StackAccountId" to Output(awsAccountId))
</pre>

```yaml
Outputs:
  StackAccountId:
    Value:
      Ref: "AWS::AccountId"
```