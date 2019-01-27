---
layout: default
title: Select
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

# Select

The `Fn::Select` function in CloudFormation selects a single value from a list of values given an index.

> Info on `Fn::Select` from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-select.html)

In KloudFormation you can use the `Select` class.

<pre class="kotlin" data-highlight-only>
val vpcId = parameter&lt;String&gt;("VPCID")
val dbSubnetIpBlocks = parameter("DbSubnetIpBlocks", ParameterType.CommaDelimitedListParameter, default = "10.0.48.0/24, 10.0.112.0/24, 10.0.176.0/24")
subnet(Select(+"0", Reference(dbSubnetIpBlocks.logicalName)), vpcId.ref())
</pre>

Produces

```yaml
Parameters:
  VPCID:
    Type: "String"
  DbSubnetIpBlocks:
    Type: "CommaDelimitedList"
    Default: "10.0.48.0/24, 10.0.112.0/24, 10.0.176.0/24"
Resources:
  Subnet:
    Type: "AWS::EC2::Subnet"
    Properties:
      CidrBlock:
        Fn::Select:
        - "0"
        - Ref: "DbSubnetIpBlocks"
      VpcId:
        Ref: "VPCID"
```



