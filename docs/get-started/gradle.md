---
layout: default
title: Gradle
parent: Get Started
nav_order: 0
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
# Gradle

## Enable the KloudFormation Gradle plugin
Add the plugin to your build.gradle.kts file.

Build script snippet for plugins DSL for Gradle 2.1 and later:

<pre class="kotlin" data-highlight-only>
plugins {
    id("io.klouds.kloudformation.gradle.plugin") version "0.1.2"
}
</pre>

Build script snippet for use in older Gradle versions or where dynamic configuration is required:
<pre class="kotlin" data-highlight-only>
buildscript {
  repositories {
    maven {
      url = uri("https://plugins.gradle.org/m2/")
    }
  }
  dependencies {
    classpath("gradle.plugin.io.klouds.kloudformation.gradle.plugin:kloudformation-gradle-plugin:0.1.2")
  }
}
    
apply(plugin = "io.klouds.kloudformation.gradle.plugin")
</pre>

This will provide your Gradle build with a new task called generateTemplate. Add this task to your main build flow.

When your project uses the Gradle Java plugin:
<pre class="kotlin" data-highlight-only>
tasks["jar"].dependsOn("generateTemplate")
</pre>

## KloudFormation Gradle plugin configuration
Add the configuration to your build.gradle.kts file.

Configure the plugin to outline your template stack:

<pre class="kotlin" data-highlight-only>
configure&lt;KloudFormationConfiguration&gt; {
    template = KloudFormationTemplate.create {
        val topicName = parameter&lt;String&gt;("TopicName")
        topic {
            topicName(topicName.ref())
        }
        queue()
        bucket {
            bucketName("myBucket")
        }
    }
}
</pre>

The path and template format is also configurable.

The above configuration will produce the following template YAML in the default path of "build/generated/template/template.yaml"

```yaml
AWSTemplateFormatVersion: "2010-09-09"
Parameters:
  TopicName:
    Type: "String"
Resources:
  Topic:
    Type: "AWS::SNS::Topic"
    Properties:
      TopicName:
        Ref: "TopicName"
  Queue:
    Type: "AWS::SQS::Queue"
  Bucket:
    Type: "AWS::S3::Bucket"
    Properties:
      BucketName: "myBucket"
```