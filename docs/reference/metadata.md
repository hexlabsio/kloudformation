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
To add metadata json to resources you can set the metatdata property on `ResourceProperties` passed to any resource.

<pre class="kotlin" data-highlight-only>
val instanceProperties = ResourceProperties(
    metadata = json(mapOf(
        . . .
    ))
)
instance(resourceProperties = instanceProperties) { 
    imageId("ami-1232131")
}
</pre>

Some resources have special metadata keys that can be applied. For example the instance resource can be supplied with `AWS::Cloudformation::Init` to provide initialisation information to the instance.

### AWS::CloudFormation::Init

> Info on AWS::CloudFormation::Init from AWS can be found [here](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-resource-init.html)

Inside the map passed to the `json` function you can invoke the `cfnInitMetadata { }` function.

<pre class="kotlin" data-highlight-only>
val instanceProperties = ResourceProperties(
    metadata = json(mapOf(
        cfnInitMetadata { 
            . . .
        }
    ))
)
instance(resourceProperties = instanceProperties) {
    imageId("ami-1232131")
}
</pre>    
        
##### ConfigSets

<pre class="kotlin" data-highlight-only>
cfnInitMetadata {
    // Config Set with one config named 1
    configSet("test1", +"1")
    // Config Set with one config set named test1 and one config named 2
    configSet("test2", configSetRef("test1"), +"2")
    // A default Config Set referencing Config Set named test2
    defaultConfigSet(configSetRef("test2"))
}
</pre>

##### Config

<pre class="kotlin" data-highlight-only>
cfnInitMetadata { 
    . . .
    // Config named 1
    config("1") { . . . }
    // Default Config
    defaultConfig { . . . }
}
</pre>

###### Commands

<pre class="kotlin" data-highlight-only>
cfnInitMetadata {
    defaultConfig {
        command(name = "test", command = +"echo \"${'$'}MAGIC\" > test.txt") {
            env("MAGIC" to +"I come from the environment!")
            cwd("~")
            test("test ! -e ~/test.txt")
            ignoreErrors(false)
        }
    }
}
</pre>


###### Files

<pre class="kotlin" data-highlight-only>
defaultConfig {
    files { 
        "/tmp/setup.mysql"(
                content = +"CREATE DATABASE " + dbName.ref() + ";\n" +
                        "CREATE USER '" + dbUsername.ref() + "'@'localhost' IDENTIFIED BY '" + dbPassword.ref() + "';\n"
        ){ 
            mode("000644")
            owner("root")
            group("root")
        }
    }
}
</pre>

###### Groups

<pre class="kotlin" data-highlight-only>
defaultConfig {
    groups { 
        "groupOne" { }
        "groupTwo" { gid("45") }
    }
}
</pre>

###### Packages

<pre class="kotlin" data-highlight-only>
defaultConfig {
    packages {
        PackageManager.rpm {
            "epel"("http://download.fedoraproject.org/pub/epel/5/i386/epel-release-5-4.noarch.rpm")
        }
        PackageManager.yum {
            "httpd"(emptyList())
            "php"(emptyList())
            "wordpress"(emptyList())
        }
        PackageManager.rubygems {
            "chef"(listOf("0.10.2"))
        }
        "other" {
            "package"("location")
        }
    }
}
</pre>

###### Services

<pre class="kotlin" data-highlight-only>
defaultConfig {
    services {
        "sysvinit" {
            "nginx" {
                enabled(true)
                ensureRunning(true)
                files(listOf(+"/etc/nginx/nginx.conf"))
                sources(listOf(+"/var/www/html"))
            }
        }
    }
}
</pre>

###### Sources

<pre class="kotlin" data-highlight-only>
defaultConfig {
    source("/etc/puppet", "https://github.com/user1/cfn-demo/tarball/master")
}
</pre>

###### Users

<pre class="kotlin" data-highlight-only>
defaultConfig {
    users {
        "myUser"(
            uid = +"50g",
            groups = +listOf(+"groupOne", +"groupTwo"),
            homeDir = +"/tmp"
        )
    }
}
</pre>

