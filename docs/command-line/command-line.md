---
layout: default
title: Command Line
nav_order: 1
---
<script src="https://unpkg.com/kotlin-playground@1" data-selector=".kotlin"></script>

## Table of contents
{: .no_toc .text-delta }

* TOC
{:toc}

# Command Line Interface
{:.no_toc}

The Command Line Interface (CLI) for KloudFormation can be downloaded directly from [here.](https://install.kloudformation.hexlabs.io/kloudformation.sh)

Alternatively, You can install it to your machine with the following command:

```bash
$ curl -sSL https://install.kloudformation.hexlabs.io | bash
```

Or, if you have limited privileges you can download to the current directory and run locally using: 

```bash
$ curl -sSL https://install.kloudformation.hexlabs.io | bash -s -- -local
```

## Command Line Options

|---|---|
|**Help**|Prints this information|
|*Option*| help |
|**Get Version**|Prints the Version of KloudFormation|
|*Option*| version |
|**Set Version**|Sets KloudFormation Version|
|*Option*| -version, -v <<Version>> |
|**Updating**|Downloads the latest version of this script and installs it|
|*Option*|update|
|**Quite**|Makes logging less verbose|
|*Option*| -quite, -q |
|**Init**|Initialise a Stack with class name matching -stack-class and filename matching -stack-file|
|*Option*| init |
|**Stack File**|Name of Kotlin file containing your stack code|
|*Option*| -stack-file <<File Name>>|
|*Default*|Stack.kt|
|**Stack Class**|Name of the class inside -stack-file implementing io.kloudformation.StackBuilder|
|*Option*|-stack-class <<Class Name>>|
|*Default*|Stack|
|**Template Output**|Name of the output template file|
|*Option*| -template <<Template File Name>>|
|*Default*|template.yml|
|**Adding Modules**|Includes a KloudFormation Module Named kloudformation-<<Module>>-module|
|*Option*| -module, -m <<Module>>@<<Version>>|
|*Example*|`kloudformation -m serverless@0.1.2 -m s3@0.1.8`|

