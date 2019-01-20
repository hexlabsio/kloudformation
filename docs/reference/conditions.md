---
layout: default
title: Conditions
parent: Reference
nav_order: 4
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

# Conditions

A condition can be used to determine if a resource should be created or not.

> Info on Conditions from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/conditions-section-structure.html)

For example, if you want to create an instance in the production environment only, do this:

<pre class="kotlin" data-highlight-only>
val environment = parameter&lt;String&gt;("Environment")
val inProduction = "InProduction"
conditions(inProduction to (environment.ref() eq +"prod"))
instance(resourceProperties = ResourceProperties(condition = inProduction)){
    instanceType("m1.small")
}
</pre>

This will produce the following:

```yaml
Parameters:
  Environment:
    Type: "String"
Conditions:
  InProduction:
    Fn::Equals:
    - Ref: "Environment"
    - "prod"
Resources:
  Instance:
    Type: "AWS::EC2::Instance"
    Condition: "InProduction"
    Properties:
      InstanceType: "m1.small"
```

## Conditional Intrinsic Functions

CloudFormation allows the use of intrinsic functions when creating a Condition

Here is a list of them and what they are called in KloudFormation:

| CloudFormation | KloudFormation |
|---|---|---|
|`Fn::And`|`and`|
|`Fn::Or`|`or`|
|`Fn::Equals`|`eq`|
|`Fn::Not`|`not()`|
|`Fn::If`|`If()`|

#### And

<pre class="kotlin" data-highlight-only>
conditions("InProdAndSizeIsHuge" to ((environment.ref() eq +"prod") and (size.ref() eq +"huge")))
</pre>

#### Or

<pre class="kotlin" data-highlight-only>
conditions("InDevOrTest" to ((environment.ref() eq +"dev") or (environment.ref() eq +"test")))
</pre>

#### Equals

<pre class="kotlin" data-highlight-only>
conditions("InProduction" to (environment.ref() eq +"prod"))
</pre>

#### Not

<pre class="kotlin" data-highlight-only>
conditions("NotInProd" to not(environment.ref() eq +"prod"))
</pre>

#### If

The If function can be used in conjunction with a Condition. It will pick the first argument if the Condition is true and the second if it is false.

<pre class="kotlin" data-highlight-only>
instance{
    //inProduction is a Condition name
    instanceType(If(inProduction, +"m1.large", +"t2.micro"))
}
</pre>

##### AWS::NoValue

The If function can be useful in order to remove a value based on a condition. This can be done by using the `awsNoValue()` function

<pre class="kotlin" data-highlight-only>
val dbSnapshotName = parameter&lt;String&gt;("DBSnapshotName")
val environment = parameter&lt;String&gt;("Environment")
val useDbSnapshot = "UseDBSnapshot"
conditions(useDbSnapshot to (environment.ref() eq +"prod"))
dBInstance(+"db.m1.small"){
    engine("MySQL")
    engineVersion("5.5")
    . . .
    dBSnapshotIdentifier(If(useDbSnapshot, dbSnapshotName.ref(), awsNoValue()))
}
</pre>

This produces the following template

```yaml
Parameters:
  DBSnapshotName:
    Type: "String"
  Environment:
    Type: "String"
Conditions:
  UseDBSnapshot:
    Fn::Equals:
    - Ref: "Environment"
    - "prod"
Resources:
  DBInstance:
    Type: "AWS::RDS::DBInstance"
    Properties:
      Engine: "MySQL"
      EngineVersion: "5.5"
      DBInstanceClass: "db.m1.small"
      DBSnapshotIdentifier:
        Fn::If:
        - "UseDBSnapshot"
        - Ref: "DBSnapshotName"
        - Ref: "AWS::NoValue"
```
        