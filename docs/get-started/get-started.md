---
layout: default
title: Get Started
nav_order: 0
has_children: true
---
<script src="https://unpkg.com/kotlin-playground@1" data-selector=".kotlin"></script>
<img style="margin: -2em; margin-left: -4em" src="../kloud-formation-logo-white.png"/>
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

# KloudFormation

KloudFormation is a one-to-one mapping of Amazon's CloudFormation generated into a Kotlin library allowing users to write type safe stacks in Kotlin.

KloudFormation can be invoked in many ways allowing any project to use it, not just Kotlin projects.

**Benefits Include**

 * Type Safety
 * Code Completion
 * Stacks as Code
 * Modular Templating
 * Up to Date
 
 > Full List of Resources [here](../reference/resources.html#full-resource-list)

## This is what it looks like
{: .no_toc }

<pre class="kotlin" data-highlight-only>
val template = KloudFormationTemplate.create {
    val topic = topic()
    bucket {
        bucketName(topic.TopicName())
    }
}
</pre>

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

If using Linux or Mac you can install and run KloudFormation as follows:

```bash
$ curl -sSL https://install.kloudformation.hexlabs.io | bash
```

Then this to see how it works:
```bash
$ kloudformation help
```

For Reference Material: See [Reference](../reference/reference.html)

To Invert a CloudFormation Template into Kloudformation: See [Inverting CloudFormation](../inversion)

For Advanced Material like Templatisation of KloudFormation: See [Advanced](../advanced/advanced.html)

For getting started with KloudFormation and your particular build tool: See links below