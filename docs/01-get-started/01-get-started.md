---
layout: default
title: Get Started
nav_order: 0
has_children: true
permalink: /kloudformation/docs/get-started
---
<script src="https://unpkg.com/kotlin-playground@1" data-selector=".kotlin"></script>
<img style="margin: -2em; margin-left: -4em" src="../../../kloud-formation-logo-white.png"/>

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

# KloudFormation

KloudFormation is a one-to-one mapping of Amazon's Cloudformation generated into a Kotlin library allowing users to write type safe stacks in Kotlin.

KloudFormation can be invoked in many ways allowing any project to use it, not just Kotlin projects.

**Benefits Include**

 * Type Safety
 * Code Completion
 * Stacks as Code
 * Modular Templating
 * Up to Date

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

Produces

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