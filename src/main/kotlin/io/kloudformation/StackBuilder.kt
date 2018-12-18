package io.kloudformation

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.kloudformation.model.KloudFormationTemplate
import java.io.File

typealias KloudFormation = KloudFormationTemplate.Builder

fun main(args: Array<String>) {
    fun arg(name: String, index: Int, default: String? = null) = if(index >= args.size) {
        default ?: throw IllegalArgumentException("$name argument is required at position $index")
    } else args[index]
    val className = arg("Class name", 0)
    val yaml = arg("JSON/YAML", 2, "yaml") == "yaml"
    val templateOutputLocation = arg("Template location", 1, "template" + if(yaml) "yml" else "json")
    val stackBuilderClass = StackBuilder::class.java.classLoader.loadClass(className)
    val builder = KloudFormationTemplate.Builder()
    stackBuilderClass.declaredMethods[0].invoke(stackBuilderClass.newInstance(), builder)
    val template = builder.build()
    File(templateOutputLocation).writeText(if(yaml) template.toYaml() else template.toJson())
}

fun kloudFormationMapper(yaml: Boolean): ObjectMapper = (if(yaml)ObjectMapper(YAMLFactory()) else ObjectMapper())
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .setPropertyNamingStrategy(KloudFormationTemplate.NamingStrategy())

interface StackBuilder{
    fun KloudFormation.create()
}

fun KloudFormationTemplate.toYaml() = kloudFormationMapper(yaml = true).writerWithDefaultPrettyPrinter().writeValueAsString(this)
fun KloudFormationTemplate.toJson() = kloudFormationMapper(yaml = false).writerWithDefaultPrettyPrinter().writeValueAsString(this)