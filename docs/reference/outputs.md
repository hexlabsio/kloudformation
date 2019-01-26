---
layout: default
title: Outputs
parent: Reference
nav_order: 6
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

# Outputs

Outputs can be added to allow importing into another stack.

> Info on Outputs from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/outputs-section-structure.html)

Outputs can be added by calling the `outputs` function.

<pre class="kotlin" data-highlight-only>
outputs(
    "StackVPC" to Output(
            value = vpc.ref(),
            description = "The ID of the VPC",
            export = Output.Export(awsStackName + "-VPCID")
    )
)
</pre>

Produces

```yaml
Outputs:
  StackVPC:
    Value:
      Ref: "VPC"
    Description: "The ID of the VPC"
    Export:
      Name:
        Fn::Join:
        - ""
        - - Ref: "AWS::StackName"
          - "-VPCID"
```