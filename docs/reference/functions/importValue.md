---
layout: default
title: ImportValue
parent: Intrinsic Functions
grand_parent: Reference
nav_order: 8
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

# ImportValue

The `Fn::ImportValue` function in CloudFormation imports a value that has been previously exported from another stacks outputs section

> Info on `Fn::ImportValue` from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-importvalue.html)

In KloudFormation you can use the `ImportValue` class.

If another stack exports a VPC Id like this:

<pre class="kotlin" data-highlight-only>
outputs(
    "StackVPC" to Output(
            value = vpc.ref(),
            description = "The ID of the VPC",
            export = Output.Export(awsStackName + "-VPCID")
    )
)
</pre>

Then this can be imported as follows:

<pre class="kotlin" data-highlight-only>
ImportValue(otherStackName.ref() + "-VPCID")
</pre>



