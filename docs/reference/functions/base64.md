---
layout: default
title: Base64
parent: Intrinsic Functions
grand_parent: Reference
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

# Base64

Creates a Base64 encoded representation of whatever is passed to it.

Info on `Fn::Base64` from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-base64.html)

In KloudFormation you can use the `FnBase64` function

<pre class="kotlin" data-highlight-only>
FnBase64(someString.ref())
</pre>



