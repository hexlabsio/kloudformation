---
layout: default
title: Sub
parent: Intrinsic Functions
grand_parent: Reference
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

# Sub

In CloudFormation `Fn::Sub` is used to substitute variables into a template string.

> Info on `Fn::Sub` from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/intrinsic-function-reference-sub.html)

In KloudFormation you need to create an instance of `Sub`.

> For KloudFormation, it is recommended to use the simpler [Join](./join.html)

<pre class="kotlin" data-highlight-only>
val rootDomainName = parameter&lt;String&gt;("RootDomainName")
outputs(
        "Domain" to Output(Sub("www.${'$'}{Domain}", mapOf("Domain" to rootDomainName.ref())))
)
</pre>

Produces

```yaml
Parameters:
  RootDomainName:
    Type: "String"
Resources: {}
Outputs:
  Domain:
    Value:
      Fn::Sub:
      - "www.${Domain}"
      - Domain:
          Ref: "RootDomainName"
```



