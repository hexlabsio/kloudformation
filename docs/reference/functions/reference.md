---
layout: default
title: Ref
parent: Intrinsic Functions
grand_parent: Reference
nav_order: 0
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

# References

All resources and parameters can be referenced by calling the `ref()` function. 

<pre class="kotlin" data-highlight-only>
val instance = instance()
eIP { 
    instanceId(instance.ref())
}
</pre>

Produces

```yaml
Resources:
  Instance:
    Type: "AWS::EC2::Instance"
  EIP:
    Type: "AWS::EC2::EIP"
    Properties:
      InstanceId:
        Ref: "Instance"
```

You can also build an instance of `Reference` passing the logical name of the resource.

<pre class="kotlin" data-highlight-only>
val instance = instance()
eIP {
    instanceId(Reference(instance.logicalName))
}
</pre>

> For Pseudo Parameters References, see [Pseudo Parameters](../../reference/parameters.html#pseudo-parameters)