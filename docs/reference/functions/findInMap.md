---
layout: default
title: FindInMap
parent: Intrinsic Functions
grand_parent: Reference
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

# Find in Map

The `Fn::FindInMap` function in CloudFormation allows lookups from the mappings section.

> Info on `Fn::FindInMap` from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-findinmap.html)

KloudFormation provides the `FindInMap` class. Here is an example of looking up machine images based on region and AMI architecture:

<pre class="kotlin" data-highlight-only>
val regionMappings = "RegionMap"
val usEast1 = "us-east-1"
val usWest1 = "us-west-1"
val hvm64 = "HVM64"
val hvmG2 = "HVMG2"
mappings(
        regionMappings to mapOf(
                usEast1 to mapOf(
                        hvm64 to +"ami-0ff8a91507f77f867",
                        hvmG2 to +"ami-0a584ac55a7631c0c"
                ),
                usWest1 to mapOf(
                        hvm64 to +"ami-0bdb828fd58c52235",
                        hvmG2 to +"ami-066ee5fd4a9ef77f1"
                )
        )
)

instance {
    instanceType("m1.small")
    imageId(FindInMap(+regionMappings, awsRegion, +hvm64))
}
</pre>

Produces

```yaml
Mappings:
  RegionMap:
    us-east-1:
      HVM64: "ami-0ff8a91507f77f867"
      HVMG2: "ami-0a584ac55a7631c0c"
    us-west-1:
      HVM64: "ami-0bdb828fd58c52235"
      HVMG2: "ami-066ee5fd4a9ef77f1"
Resources:
  Instance:
    Type: "AWS::EC2::Instance"
    Properties:
      ImageId:
        Fn::FindInMap:
        - "RegionMap"
        - Ref: "AWS::Region"
        - "HVM64"
      InstanceType: "m1.small"
```



