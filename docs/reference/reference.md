---
layout: default
title: Reference
nav_order: 2
has_children: true
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


# KloudFormation Reference

You can find a reference to every part of KloudFormation within this section with examples showing how to make use of it in your stack.

> For Template creation and general idioms, see [Fundamentals](./fundamentals.html)

Here is a list of equivalents and where to find each part in the reference

| CloudFormation | KloudFormation | Reference |
|---|---|---|
|Metadata|metadata()|[Metadata](./metadata.html)|
|Parameters|parameter<T>()|[Parameters](./parameters.html)|
|Mappings|mappings()|[Mappings](./mappings.html)|
|Conditions|condition()|[Conditions](./conditions.html)|
|Resources|io.kloudformation.resource.*|[Resources](./resources.html)|
|Outputs|outputs()|[Outputs](./outputs.html)|

A list of Intrinsic Function like `Ref` and `Fn::Join` can be found under [Intrinsic Functions](./functions/functions.html)




