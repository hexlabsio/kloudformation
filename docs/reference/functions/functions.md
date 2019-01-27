---
layout: default
title: Intrinsic Functions
parent: Reference
nav_order: 8
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

## Table of contents
{: .no_toc .text-delta }

* TOC
{:toc}

# Intrinsic Functions

> Info on Intrinsic Functions from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference.html)

| CloudFormation | KloudFormation Option A | KloudFormation Option B | Reference |
|---|---|---|---|
|Ref|Reference(name)|resource.ref()|[Ref](./reference.html)|
|Fn::GetAtt|GetAtt(resource, attribute)|resource.&lt;Attribute Name&gt;()|[GetAtt](./attributes.html)|
|Fn::Join|Join(splitter, listOf(a,b,c))|resource.ref() + "string"|[Join](./join.html)|
|Fn::Sub|Sub(subString, mapOf(variables))||[Sub](./sub.html)|
|Fn::Base64|FnBase64(+"string")||[Base64](./base64.html)|
|Fn::Cidr|||[Cidr](./cidr.html)|
|Fn::FindInMap|||[FindInMap](./findInMap.html)|
|Fn::GetAZs|||[GetAZs](./getAzs.html)|
|Fn::ImportValue|||[ImportValue](./importValue.html)|
|Fn::Select|||[Select](./select.html)|
|Fn::Split|||[Split](./split.html)|