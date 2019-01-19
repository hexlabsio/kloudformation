---
layout: default
title: Mappings
parent: Reference
nav_order: 3
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

# Mappings

The Mappings section matches a key to a corresponding set of named values

> Info on Mappings from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/mappings-section-structure.html)

You can create a set of mappings by invoking the `mappings()` function

> All Values are the Value&lt;T&gt; type, see [The Value&lt;T&gt; Type](../reference/fundamentals.html#the-valuet-type)

<pre class="kotlin" data-highlight-only>
mappings(
        "RegionMap" to mapOf(
                "us-east-1" to mapOf(
                        "HVM64" to +"ami-0ff8a91507f77f867",
                        "HVMG2" to +"ami-0a584ac55a7631c0c"
                ),
                "us-west-1" to mapOf(
                        "HVM64" to +"ami-0bdb828fd58c52235",
                        "HVMG2" to +"ami-066ee5fd4a9ef77f1"
                )
        )
)
</pre>

A map lookup can be done by passing an instance of FindInMap

<pre class="kotlin" data-highlight-only>
instance {
    instanceType("m1.small")
    imageId(FindInMap(+"RegionMap", awsRegion, +"HVM64"))
}
</pre>

Produces the following Template

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