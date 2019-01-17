---
layout: default
title: Templating
parent: Advanced
nav_order: 2
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

1. TOC
{:toc}

# Templating

KloudFormation provides a way to build templates that modularise parts of Cloudformation for users.

For example, building an S3 website is a regular task that can be wrapped into a function.

<pre class="kotlin" data-highlight-only>
    s3Website { }
</pre>

Using the pattern outlined here, you can grant the user access to all of the parts that make up a module by designing a template.

The user experience looks like this:

<pre class="kotlin" data-highlight-only>
// s3Website is a Module that templates the parts that make up a website deployed to S3
s3Website {
    // s3Bucket is one of the parts making up an s3Website
    s3Bucket {
        props { errorDocument = "404.html" } // Access to modify properties specified by the template builder
        modify { bucketName("NewBucketName") } // Full access to KloudFormation here
    }
    s3Distribution(+"klouds.io") // An optional Submodule attaches a domain
}
</pre>

# Building a Module

A **Module** is simply a packaged up set of KloudFormation steps that create Cloudformation template parts.

Modules are made up of **Parts** that can be modified by the user.

A Part can be anything but will most likely be a Cloudformation Resource or a **SubModule**

A **SubModule** is a **Module** that requires **Predefined** information from a parent **Module** in order to build its **Parts**

Here is the basic structure of a Module with no parts

<pre class="kotlin" data-highlight-only>
class MyModule: Module {
    class Parts
    class Builder: ModuleBuilder<MyModule, Parts>(Parts()){
        override fun KloudFormation.buildModule(): Parts.() -> MyModule = {
            // KloudFormation here
            MyModule()
        }
    }
}
val myModule = builder(MyModule.Builder())
</pre>

The user can then invoke that within KloudFormation as follows:

<pre class="kotlin" data-highlight-only>
// The Parts instance is passed as the receiver inside the brackets
// This will be useful later when we include parts
myModule { }
</pre>

## The Parts Class

The Parts class represents the individual parts that go into making a Module

For example an S3Bucket is a part of the S3Website Module

Anything within the Parts class will be passed to the user

A Part can be represented using the Modification type. Here is an example of the s3Bucket part within the S3Website Module

<pre class="kotlin" data-highlight-only>
    val s3Bucket = modification<Bucket.Builder, Bucket, BucketProps>()
</pre>

## The Modification Class

A Modification represents a single item that the user has access to modify.

A Modification can be created using one of the `modification()` or `optionalModification()` functions

It takes three type parameters, a `Builder`, the thing your building and a `Properties` type

> The `Properties` type should have mutable variables

Modifications present to the user granting them the opportunity to modify properties and have access to the Builder of the resource

<pre class="kotlin" data-highlight-only>
// When a modification is invoked by the user it surfaces two methods ( props and modify )
s3Bucket {
    props { /* this is BucketProps */ }
    modify { /* this is Bucket.Builder */ }
}
</pre>

## S3 Website Example

. . .