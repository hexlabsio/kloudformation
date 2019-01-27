---
layout: default
title: GetAtt
parent: Intrinsic Functions
grand_parent: Reference
nav_order: 1
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

# Attributes
If a resource has attributes associated to it then in CloudFormation you would use `Fn::GetAtt` listing the resource and the attribute you wanted.

> Info on Fn::GetAtt from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-getatt.html)

In KloudFormation all attributes are provided as functions on the resource.

<pre class="kotlin" data-highlight-only>
val securityGroup = securityGroup(+"Description")
outputs(
        "GroupId" to Output(securityGroup.GroupId()), // GroupId is an Attribute
        "VpcId" to Output(securityGroup.VpcId()) // VpcId is an Attribute
)
</pre>

Produces

```yaml
Resources:
  SecurityGroup:
    Type: "AWS::EC2::SecurityGroup"
    Properties:
      GroupDescription: "Description"
Outputs:
  GroupId:
    Value:
      Fn::GetAtt:
      - "SecurityGroup"
      - "GroupId"
  VpcId:
    Value:
      Fn::GetAtt:
      - "SecurityGroup"
      - "VpcId"
```
You can also create an instance of `Att` as follows:

<pre class="kotlin" data-highlight-only>
Att&lt;String&gt;(securityGroup.logicalName,+"VpcId")
</pre>


