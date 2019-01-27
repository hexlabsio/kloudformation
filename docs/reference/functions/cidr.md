---
layout: default
title: Cidr
parent: Intrinsic Functions
grand_parent: Reference
nav_order: 5
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

# Cidr

The `Fn::Cidr` function in CloudFormation takes a Cidr block and splits it into smaller chunks. The result is a list of Cidr Blocks

> Info on `Fn::Cidr` from CloudFormation can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-cidr.html)

This example create 6 CIDRs with a subnet mask /27 from a CIDR with a mask of /24.

<pre class="kotlin" data-highlight-only>
Cidr(+"192.168.0.0/24", +"6", +"5")
</pre>




