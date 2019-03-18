---
layout: default
title: No Build Tool
parent: Get Started
nav_order: 2
---

<script src="https://unpkg.com/kotlin-playground@1"
 data-selector=".kotlin"
 data-server="https://playground.hexlabs.io"></script>
<style>
blockquote{
    color: #666;
    margin: 0;
    padding-left: 3em;
    border-left: 0.5em #f2c152 solid;
}
</style>

# No Build Tool

You don't need a build tool to run KloudFormation. You can install Kloudformation on mac / linux by opening a terminal and running the following command

```bash
curl -s https://install.kloudformation.hexlabs.io | sh
```

Then this to see how it works:
```bash
kloudformation help
```

## Manually

Alternatively if gradle, maven or the above options don't suit you can try running manually with the uber jar.

You can create a template from anywhere as long as you have **kotlinc** and **java** available.

Make sure you build a stack by implementing `StackBuilder`

<pre class="kotlin">
import io.kloudformation.model.KloudFormationTemplate
import io.kloudformation.toYaml
//sampleStart
import io.kloudformation.KloudFormation
import io.kloudformation.StackBuilder

class Stack: StackBuilder {
    override fun KloudFormation.create() {  }
}
//sampleEnd

fun main(){
 println(KloudFormationTemplate.create {
         Stack().run { create() }
 }.toYaml())
}
</pre>

Download the latest KloudFormation jar from bintray.

> Make sure to download the **uber** jar from [Bintray](https://bintray.com/hexlabsio/kloudformation/kloudformation/_latestVersion#files)

The steps required to generate your template are as follows:

1. Place the file containing your Stack class into the same directory as the KloudFormation uber jar.
2. Rename the uber jar to kloudformation.jar
3. Compile your stack class using the following command:
    ```bash
    kotlinc -cp kloudformation.jar <your-stack-file>.kt -include-runtime -d stack.jar
    ```
4. Run the `StackBuilder` to generate your stack template:
    ```bash
    java -classpath stack.jar:kloudformation.jar io.kloudformation.StackBuilderKt Stack template.yml
    ```

Your template will be in a file called template.yml