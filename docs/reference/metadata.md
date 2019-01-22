---
layout: default
title: Metadata
parent: Reference
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

# Metadata

You can use the metadata section to provide arbitrary YAML or JSON

> Info on Metadata from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/metadata-section-structure.html)

The `metadata()` function can take a JsonNode or you can make use of the [`json()`](./fundamentals.html#the-valuejsonnode-type) function.

<pre class="kotlin" data-highlight-only>
metadata(json(mapOf(
        "Instances" to mapOf("Description" to "Information about the instances"),
        "Databases" to mapOf("Description" to "Information about the databases")
)))
</pre>

Produces

```yaml
Metadata:
  Instances:
    Description: "Information about the instances"
  Databases:
    Description: "Information about the databases"
```

## Metadata on Resources
Some resources have special metadata keys that can be applied. For example the instance resource can be supplied with `AWS::Cloudformation::Init` to provide initialisation information to the instance.

### AWS::CloudFormation::Init

> Info on AWS::CloudFormation::Init from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-init.html)

##### ConfigSets

#### Coming Soon
{: .no_toc }

##### Config

#### Coming Soon
{: .no_toc }

###### Commands

#### Coming Soon
{: .no_toc }

###### Files

#### Coming Soon
{: .no_toc }

###### Groups

#### Coming Soon
{: .no_toc }

###### Packages

#### Coming Soon
{: .no_toc }

###### Services

#### Coming Soon
{: .no_toc }

###### Sources

#### Coming Soon
{: .no_toc }

###### Users

#### Coming Soon
{: .no_toc }


