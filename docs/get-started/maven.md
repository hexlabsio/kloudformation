---
layout: default
title: Maven
parent: Get Started
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
# Maven

This example will set up Maven to run KloudFormation before the package phase. The source for the stack will be added as test sources so that the stack code is not included in the jar.

> The directories and names listed can all be customised.

1. Create a directory under the **src** folder named **stack**
2. Add a new file under the **stack** folder named **Stack.kt**
3. In **Stack.kt** create a class named **Stack** that implements `io.kloudformation.StackBuilder`
        
    <pre class="kotlin" data-highlight-only>
    class Stack: StackBuilder {
        override fun KloudFormation.create() { . . . }
    }
    </pre>

4. Add the following to pom.xml:
         ( Latest Version <a href='https://bintray.com/hexlabsio/kloudformation/kloudformation'><img style="height: 0.9em" src='https://api.bintray.com/packages/hexlabsio/kloudformation/kloudformation/images/download.svg'></a> )
    
    a. Include KloudFormation as a test scoped dependency
    
    ```xml
    <dependency>
        <groupId>io.kloudformation</groupId>
        <artifactId>kloudformation</artifactId>
        <version>${kloudformation.version}</version>
        <scope>test</scope>
    </dependency>
    ```

    b. Add or Update the kotlin-maven-plugin to include the stack directory under test-compile
    
    ```xml
    <plugin>
        <artifactId>kotlin-maven-plugin</artifactId>
        <groupId>org.jetbrains.kotlin</groupId>
        <version>${kotlin.version}</version>
        <executions>
            <execution>
                <id>compile</id>
                <goals>
                    <goal>compile</goal>
                </goals>
                <configuration>
                    <sourceDirs>
                        <sourceDir>${project.basedir}/src/main/kotlin</sourceDir>
                    </sourceDirs>
                </configuration>
            </execution>
            <execution>
                <id>test-compile</id>
                <goals>
                    <goal>test-compile</goal>
                </goals>
                <configuration>
                    <sourceDirs>
                        <sourceDir>${project.basedir}/src/test/kotlin</sourceDir>
                        <sourceDir>${project.basedir}/src/stack</sourceDir>
                    </sourceDirs>
                </configuration>
            </execution>
        </executions>
    </plugin>
    ```
    
    c. Invoke StackBuilder to generate the Cloudformation Template while processing test sources

    ```xml
    <plugin>
        <groupId>org.codehaus.mojo</groupId>
        <artifactId>exec-maven-plugin</artifactId>
        <version>1.4.0</version>
        <executions>
            <execution>
                <phase>process-test-classes</phase>
                <goals>
                    <goal>java</goal>
                </goals>
                <configuration>
                    <mainClass>io.kloudformation.StackBuilderKt</mainClass>
                    <arguments>
                        <argument>Stack</argument>
                        <argument>${project.basedir}/template.yml</argument>
                    </arguments>
                    <classpathScope>test</classpathScope>
                </configuration>
            </execution>
        </executions>
    </plugin>
    ```
    
5. Build your stack

    <pre class="kotlin" data-highlight-only>
    
    class Stack: StackBuilder {
        override fun KloudFormation.create() {
            val topicName = parameter<String>("TopicName")
            topic {
                topicName(topicName.ref())
            }
            queue()
            bucket {
                bucketName("myBucket")
            }
        }
    }

Produces

```yaml
AWSTemplateFormatVersion: "2010-09-09"
Parameters:
  TopicName:
    Type: "String"
Resources:
  Topic:
    Type: "AWS::SNS::Topic"
    Properties:
      TopicName:
        Ref: "TopicName"
  Queue:
    Type: "AWS::SQS::Queue"
  Bucket:
    Type: "AWS::S3::Bucket"
    Properties:
      BucketName: "myBucket"
```