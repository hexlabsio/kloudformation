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
$ curl -sSL https://install.kloudformation.hexlabs.io | bash
```

Then this to see how it works:
```bash
$ kloudformation help
```


> For More Info on the Command Line Interface See [Command Line](../command-line/command-line.html)

## Code Completion in IntelliJ without Maven / Gradle

The first time you run the KloudFormation script above it will download jars into a directory named `kloudformation` wherever you ran the script.

You can then open the project in IntelliJ as follows:

1. After installing KloudFormation (Above), In a clean directory Run:

     ```bash 
     $ mkdir stack && kloudformation init -stack-file stack/Stack.kt
     ```
 
2. Transpile the generated Stack file (This will download all jars into a kloudformation directory):

    ```bash 
    $ kloudformation -stack-file stack/Stack.kt
    ``` 

3. Open the current directory in IntelliJ

4. In IntelliJ, right click on the `stack` folder and select `Mark Directory As > Sources Root`

5. In IntelliJ, right click on `kloudformation/kloudformation-x.x.xxx.jar` and select `Add as Library...` and click OK.

6. Open stack/Stack.kt and start writting your stack.


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