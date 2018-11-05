import com.jfrog.bintray.gradle.BintrayExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.bundling.Jar
import java.time.OffsetDateTime
import java.time.ZoneOffset



val projectVersion = "0.1.0"
//val projectVersion = "1.0-SNAPSHOT"
val projectDescription = """KloudFormation Specification Generator"""

val kotlinVersion = "1.3.0"
val KloudSpecGenVersion = "0.1.0"
val junitVersion = "5.0.0"
val gitPublishVersion = "5.0.0"

group = "io.kloudformation"
version = projectVersion
description = projectDescription


plugins {
    id("com.jfrog.bintray") version "1.8.4"
    kotlin("jvm") version "1.3.0"
    `maven-publish`
    `java`

}

repositories {
    jcenter()
    mavenCentral()
}



dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(group = "io.kloudformation", name = "kloudformation-specification-generator", version = kotlinVersion)
    compile(group = "org.jetbrains.kotlin", name = "kotlin-stdlib", version = kotlinVersion)
    testCompile(group = "org.junit.jupiter", name = "junit-jupiter-api", version = junitVersion)
    testCompile(kotlin("test-junit5"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}



task("genSpec", JavaExec::class) {
    main = "io.kloudformation.specification.SpecificationGeneratorKt"
}

compileKotlin {
    dependsOn = "genSpec"
}

val sourcesJar by tasks.registering(Jar::class) {
    classifier = "sources"
    from(sourceSets["main"].allSource)
}

val bintray_user: String? by project
val bintray_key: String? by project


bintray {
    user = bintray_user ?: System.getProperty("bintray_user")
    key = bintray_key ?: System.getProperty("bintray_key")
    setPublications("mavenJava")
    pkg(
            closureOf<BintrayExtension.PackageConfig> {
                repo = "kloudformation"
                name = "kloudformation-specification-generator"
                userOrg = "hexlabsio"
                setLicenses("Apache-2.0")
                vcsUrl = "https://github.com/hexlabsio/kloudformation.git"
                version(closureOf<BintrayExtension.VersionConfig> {
                    name = projectVersion
                    desc = projectDescription

                })
            })
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            groupId = project.group as String
            version = projectVersion
            artifact(sourcesJar.get())
        }
    }
/*    repositories {
        maven {
            url = "/;publish=1"
        }
    }*/
}