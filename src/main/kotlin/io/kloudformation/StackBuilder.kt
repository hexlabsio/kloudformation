package io.kloudformation

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import io.kloudformation.model.KloudFormationTemplate
import java.io.File

typealias KloudFormation = KloudFormationTemplate.Builder

fun main(args: Array<String>) {
    val (className, templateOutputLocation) = args
    val stackBuilderClass = StackBuilder::class.java.classLoader.loadClass(className)
    val builder = KloudFormationTemplate.Builder()
    stackBuilderClass.declaredMethods[0].invoke(stackBuilderClass.newInstance(), builder)
    val template = builder.build()
    File(templateOutputLocation).writeText(
            ObjectMapper(YAMLFactory())
                    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                    .setPropertyNamingStrategy(KloudFormationTemplate.NamingStrategy())
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(template)
    )
}

interface StackBuilder{
    fun KloudFormation.create()
}