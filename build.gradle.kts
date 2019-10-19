import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.jfrog.bintray.gradle.BintrayExtension
import groovy.util.Node
import groovy.util.NodeList
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.bundling.Jar
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

fun version(): String {
    val lastVersionBuildNumber= 128
    val buildNumber = System.getProperty("BUILD_NUM")?.let { (it.toInt() - lastVersionBuildNumber - 1).toString() } ?: "-SNAPSHOT"
    val version = "1.1.$buildNumber"
    println("building version $version")
    return version
}

val projectVersion = version()
val projectDescription = """KloudFormation"""

val kotlinVersion = "1.3.11"
val jacksonVersion = "2.9.8"
val kotlinpoetVersion = "1.0.1"
val junitVersion = "5.0.0"
val jsoupVersion = "1.11.3"
val kloudformationVersion = "0.1.51"

val artifactId = "kloudformation"
group = "io.kloudformation"
version = projectVersion
description = projectDescription

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.11"
    id("org.jlleitschuh.gradle.ktlint") version "6.3.1"
    id("com.jfrog.bintray") version "1.8.4"
    id("com.github.johnrengelman.shadow") version "4.0.4"
    `maven-publish`
}

repositories {
    jcenter()
    mavenCentral()
}


dependencies {
    api(group = "com.fasterxml.jackson.core", name = "jackson-databind", version = jacksonVersion)
    api(group = "com.fasterxml.jackson.module", name = "jackson-module-kotlin", version = jacksonVersion)
    api(group = "com.fasterxml.jackson.dataformat", name = "jackson-dataformat-yaml", version = jacksonVersion)
    implementation(group = "com.squareup", name = "kotlinpoet", version = kotlinpoetVersion)
    implementation(group = "org.jsoup", name = "jsoup", version = jsoupVersion)

    testImplementation(group = "org.jetbrains.kotlin", name = "kotlin-test-junit5", version = kotlinVersion)
    testImplementation(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junitVersion)
    testRuntime(group = "org.junit.jupiter", name = "junit-jupiter-engine", version = junitVersion)
}

sourceSets {
    create("specificationGenerator") {
        java {
            srcDirs("src/main/kotlin")
            compileClasspath = sourceSets["main"].compileClasspath
            runtimeClasspath = sourceSets["main"].runtimeClasspath
        }
    }
    main {
        java {
            srcDirs("src/main/kotlin", "build/generated")
        }
    }
}

val generateSpecificationSource by tasks.register<JavaExec>("generateSpecificationSource") {
    dependsOn("compileSpecificationGeneratorKotlin")
    doFirst {
        main = "io.kloudformation.specification.SpecificationGeneratorKt"
        classpath = sourceSets["main"].runtimeClasspath + sourceSets["specificationGenerator"].output
    }
}

val compileKotlin by tasks.getting(KotlinCompile::class) {
    dependsOn(generateSpecificationSource)
    kotlinOptions.jvmTarget = "1.8"
}

val test by tasks.existing(Test::class) {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    println(sourceSets["main"].allSource)
    from(sourceSets["main"].allSource)
}

val shadowJar by tasks.getting(ShadowJar::class) {
    classifier = "uber"
    manifest {
        attributes(mapOf("Main-Class" to "io.kloudformation.StackBuilderKt"))
    }
    dependencies {
        exclude(dependency("org.jetbrains.kotlin::$kotlinVersion"))
    }
}

artifacts {
    add("archives", shadowJar)
    add("archives", sourcesJar)
}

bintray {
    user = "hexlabs-builder"
    key = System.getProperty("BINTRAY_KEY") ?: "UNKNOWN"
    setPublications("mavenJava")
    publish = true
    pkg(
            closureOf<BintrayExtension.PackageConfig> {
                repo = "kloudformation"
                name = artifactId
                userOrg = "hexlabsio"
                setLicenses("Apache-2.0")
                vcsUrl = "https://github.com/hexlabsio/kloudformation.git"
                version(closureOf<BintrayExtension.VersionConfig> {
                    name = projectVersion
                    desc = projectVersion
                })
            })
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifactId = artifactId
            artifact(shadowJar)
            artifact(sourcesJar)
            pom.withXml {
                val dependencies = (asNode()["dependencies"] as NodeList)
                configurations.compile.allDependencies.forEach {
                    dependencies.add(Node(null, "dependency").apply {
                        appendNode("groupId", it.group)
                        appendNode("artifactId", it.name)
                        appendNode("version", it.version)
                    })
                }
            }
        }
    }
}

configure<KtlintExtension> {
    verbose.set(true)
    outputToConsole.set(true)
    coloredOutput.set(true)
    reporters.set(setOf(ReporterType.CHECKSTYLE, ReporterType.JSON))
    filter {
        exclude("**/property/**/*.kt")
        exclude("**/resource/**/*.kt")
    }
}