---
layout: default
title: GetAZs
parent: Intrinsic Functions
grand_parent: Reference
nav_order: 7
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

# GetAZs

The `Fn::GetAZs` function in CloudFormation returns a list of Availability Zones given a particular region.

> Info on `Fn::GetAZs` from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getavailabilityzones.html)

In KloudFormation you can use the `GetAZs` class:

<pre class="kotlin" data-highlight-only>
GetAZs(awsRegion)
</pre>



