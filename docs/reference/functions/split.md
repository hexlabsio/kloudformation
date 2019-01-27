---
layout: default
title: Split
parent: Intrinsic Functions
grand_parent: Reference
nav_order: 9
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

# Split

The `Fn::Split` function in CloudFormation allows a string value to be split by a separator. This can be used in conjunction with Select to pick one of the parts.

> Info on `Fn::Split` from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-split.html)

In KloudFormation you can use the Split class:

<pre class="kotlin" data-highlight-only>
Select(+"2", Split(",", +"A, B, C"))
</pre>



