---
layout: default
title: Join
parent: Intrinsic Functions
grand_parent: Reference
nav_order: 2
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

# Join

In CloudFormation you can concatenate Strings and intrinsic functions using `Fn::Join`

> Info of `Fn::Join` from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-join.html)

We have simplified how this works in KloudFormation by providing the plus operator. 

You can concatenate any Reference, `Value<String>` or Attribute as follows:

<pre class="kotlin" data-highlight-only>
val sgName = parameter&lt;String&gt;("Name")
securityGroup(groupDescription = +"Description-" + sgName.ref() + "-XYZ")
</pre>

> Tip: If the first element is a `String` then you need the unary plus to make it `Value<String>`

Produces

```yaml
  SecurityGroup:
    Type: "AWS::EC2::SecurityGroup"
    Properties:
      GroupDescription:
        Fn::Join:
        - ""
        - - "Description-"
          - Ref: "Name"
          - "-XYZ"
```

By default the plus operator will create an instance of `Join` and will apply no splitter between elements.

If you want to supply a splitter you will need to create an instance of `Join`:

<pre class="kotlin" data-highlight-only>
securityGroup(groupDescription = Join("-", listOf(+"Description", sgName.ref(), +"XYZ")))
</pre>


