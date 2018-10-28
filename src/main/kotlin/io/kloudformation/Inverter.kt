package io.kloudformation

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.squareup.kotlinpoet.*
import io.kloudformation.function.*
import io.kloudformation.model.KloudFormationTemplate
import io.kloudformation.specification.SpecificationPoet
import java.io.File
import java.lang.IllegalArgumentException

fun main(args: Array<String>){
    try {
        Inverter.invert(Inverter::class.java.classLoader.getResource("sandbox.yml").readText())
                .writeTo(File("/Users/chrisbarbour/Code/kloudformation/kloudformation-specification/target/generated-sources"))
    }
    catch(e: Exception){
        if(e.message != null) error(e.message.toString()) else e.printStackTrace()
    }
}

object Inverter{
    class InverterException(message: String): Exception(message)
    data class ResourceInfo(val type: String, val canonicalPackage: String, val name: String, val required: Map<String, ResourceTypeInfo>, val notRequired: Map<String, ResourceTypeInfo>)
    data class ResourceTypeInfo(val rawType: String = "", val canonicalPackage: String? = null, val className: String? = null, val required: Boolean = true, val valueType: Boolean = false, val list: Boolean = false, val map: Boolean = false, val parameterA: ResourceTypeInfo? = null, val parameterB: ResourceTypeInfo? = null)
    fun invert(template: String): FileSpec =
            ObjectMapper()
            .registerModule(SimpleModule().addDeserializer(FileSpec::class.java, StackInverter()))
            .readValue(template)

    private const val kPackage = "io.kloudformation"
    private const val className = "MyStack"
    private const val functionName = "stack"
    private val resourceInfo = resourceInfo()

    private fun String.propertyInfo(required: Boolean): ResourceTypeInfo{
        val isValue = startsWith("$kPackage.Value")
        val isList = startsWith("List") || startsWith("kotlin.collections.List")
        val isMap = startsWith("kotlin.collections.Map")
        val canonicalPackage = if(startsWith("AWS::")) canonicalPackageFor(this, false) else null
        val className = if(startsWith("AWS::")) substringAfterLast(".") else null
        val parameterA = when {
            isList || isValue -> substringAfter("<").substringBeforeLast(">")
            isMap ->  substringAfter("<").substringBefore(",")
            else -> null
        }?.propertyInfo(required)
        val parameterB = when{
            isMap ->  substringAfter(",").substringBeforeLast(">")
            else -> null
        }?.propertyInfo(required)
        return ResourceTypeInfo(this, canonicalPackage, className, required, isValue, isList, isMap, parameterA, parameterB)
    }

    private fun resourceInfo(): Map<String, ResourceInfo>{
        fun Map<String, String>.info(required: Boolean) = map { (name, type) -> name to type.propertyInfo(required) }.toMap()
        return jacksonObjectMapper().readValue<Map<String, SpecificationPoet.Info>>(Inverter::class.java.classLoader.getResource("info.json"))
            .map { (awsType, info) ->
                awsType to ResourceInfo(awsType, canonicalPackageFor(awsType, info.resource), info.name, info.required.info(true), info.notRequired.info(false))
            }.toMap()
    }

    private fun canonicalPackageFor(awsType: String, resource: Boolean): String{
        val resourcePackage = awsType.substringAfter("::").substringBefore("::").toLowerCase()
        val otherPackages = awsType.substringAfter("::").substringAfter("::").split(".")
        val otherPackagesString = otherPackages.subList(0, otherPackages.size-1).accumulate(separator = ".") { it.toLowerCase() }
        val tail = if(otherPackagesString.isNotEmpty())".$otherPackagesString" else ""
        return "$kPackage.${if(resource) "resource" else "property"}.$resourcePackage$tail"
    }

    private fun <A, S> Map<A, S>.accumulate(start: String = "", end: String = "", separator: String = ", ", firstIncluded: Boolean = false, conversion: (Map.Entry<A, S>) -> String) =
            asSequence().toList().accumulate(start, end, separator, firstIncluded, conversion)

    private fun <S> List<S>.accumulate(
            start: String = "",
            end: String = "",
            separator: String = ", ",
            firstIncluded: Boolean = false,
            conversion: (S) -> String = { "$it" }
    ): String = foldIndexed(start){ index, acc, item ->
        conversion(item).let { text -> "$acc${ if((index>0 || firstIncluded) && text.isNotEmpty()) separator else "" }$text" }
    } + end

    private class StackInverter(
            private val staticImports: MutableList<Pair<String, String>> = mutableListOf(),
            private val parameters: MutableMap<String, JsonNode> = mutableMapOf(),
            private val mappings: MutableMap<String, JsonNode> = mutableMapOf(),
            private val resources: MutableMap<String, JsonNode> = mutableMapOf()
    ): StdDeserializer<FileSpec>(FileSpec::class.java) {

        private fun <T> Map<String, T>.similar(key: String): T? = keys.find { it.equals(key, true) }?.let { this[it] }
        private fun JsonNode.elementsAsList() = elements().asSequence().toList()
        private fun JsonNode.fieldsAsList() = fields().asSequence().toList()
        private fun JsonNode.fieldsAsMap() = fields().asSequence().toList().map { (key, value) -> key to value }.toMap()
        private fun JsonNode.mapFromFieldNamed(fieldName: String) = this[fieldName]?.fieldsAsList()?.map { it.key to it.value }?.toMap() ?: emptyMap()

        private fun JsonNode.parameterType() = this["Type"]?.textValue().let {
            (it ?: "String") to when(it){
                "List<Number>" -> ParameterizedTypeName.get(List::class, String::class)
                "List<String>" -> ParameterizedTypeName.get(List::class, String::class)
                "CommaDelimitedList" -> ParameterizedTypeName.get(List::class, String::class)
                //TODO deal with others
                else -> String::class.asClassName()
            }
        }

        private fun JsonNode.properties() = this["Properties"]?.fieldsAsMap() ?: emptyMap()

        private fun JsonNode.resourceTypeInfo(name: String) = this["Type"]?.textValue()?.let {
            it to ( resourceInfo[it] ?: throw InverterException("Did not have enough information to discover type $it"))
        } ?: throw IllegalArgumentException("Could not read type of resource with logical name $name")

        private fun CodeBuilder.valueTypeFor(value: JsonNode, expectedType: ResourceTypeInfo): String{
            return if(expectedType.list){
                val items = if(value.isArray) value.elementsAsList() else listOf(value)
                items.accumulate("+listOf(\n%>", "\n%<)", ",\n") { value(it,expectedTypeInfo = expectedType.parameterA!!) }
            } else when(expectedType.rawType){
                    "kotlin.String" -> { this + value.asText(); "+%S"}
                    else -> {
                        this + Value.Of::class
                        "%T(${value.asText()})"
                    }
                }
            }

        private fun CodeBuilder.valueString(item: JsonNode, explicit: Boolean = false) = value(item, explicit = explicit, expectedTypeInfo = ResourceTypeInfo(valueType = true, parameterA = ResourceTypeInfo("kotlin.String")))

        private fun CodeBuilder.attFrom(node: JsonNode, expectedType: ResourceTypeInfo): String {
            val (resource, attribute) = node.elementsAsList()
            val resourceText = resource.textValue()
            this + Att::class
            val attResource = if((parameters.keys + resources.keys).contains(resourceText)){
                refBuilder.refs += resourceText
                resourceText.decapitalize() + ".logicalName"
            }
            else {
                this + resourceText
                "%S"
            }
            return "%T<${expectedType.rawType}>($attResource, " + valueString(attribute) + ")"
        }

        private fun CodeBuilder.joinFrom(node: JsonNode): String {
            val (splitter, items) = node.elementsAsList()
            return if(splitter.asText().isEmpty()){
                staticImports.add("$kPackage.function" to "plus")
                items.elementsAsList().map { valueString(it, true) }.accumulate(separator = " +\n") { it }
            } else {
                this + Join::class
                "%T(${splitter.asText()}, ${ items.elementsAsList().map { valueString(it) }.accumulate("listOf(\n%>", "\n%<)", separator = ", \n") { it }})"
            }
        }

        private fun CodeBuilder.base64From(node: JsonNode): String {
            this + FnBase64::class
            return "%T(" + valueString(node) + ")"
        }

        private fun CodeBuilder.refFrom(refItem: String, expectedType: ResourceTypeInfo, explicit: Boolean = false) =
            if((parameters.keys + resources.keys).contains(refItem)) {
                refBuilder.refs += refItem
                if(expectedType.rawType == "kotlin.String")  "${refItem.decapitalize()}.ref()"
                else {
                    this + Reference::class
                    "%T${if(explicit) "<${expectedType.rawType}>" else ""}(${refItem.decapitalize()}.logicalName)"
                }
            }
            else {
                val special = when(refItem){
                    "AWS::AccountId" -> "awsAccountId"
                    "AWS::NotificationARNs" -> "awsNotificationArns"
                    "AWS::NoValue" -> "awsNoValue()"
                    "AWS::Partition" -> "awsPartition"
                    "AWS::Region" -> "awsRegion"
                    "AWS::StackId" -> "awsStackId"
                    "AWS::StackName" -> "awsStackName"
                    "AWS::URLSuffix" -> "awsUrlSuffix"
                    else -> null
                }
                if(special != null){
                    staticImports.add("io.kloudformation.model.KloudFormationTemplate.Builder.Companion" to special.substringBeforeLast("()"))
                    special
                }
                else {
                    this + Reference::class
                    this + refItem
                    "%T${if (explicit) "<${expectedType.rawType}>" else ""}(%S)"
                }
            }

        private fun CodeBuilder.getAzsFrom(node: JsonNode): String {
            this + GetAZs::class
            return "%T(${valueString(node)})"
        }

        private fun CodeBuilder.findInMapFrom(node: JsonNode): String {
            val (map, top, second) = node.elementsAsList()
            this + FindInMap::class
            return "%T(${valueString(map)}, ${valueString(top)}, ${valueString(second)})"
        }

        private fun CodeBuilder.ifFrom(node: JsonNode, expectedType: ResourceTypeInfo): String {
            val (condition, ifTrue, ifFalse) = node.elementsAsList()
            this + If::class
            this + condition
            val newExpectedType = "$kPackage.Value<${expectedType.rawType}>".propertyInfo(expectedType.required)
            return "%T(%S, ${value(ifTrue,expectedTypeInfo = newExpectedType)}, ${value(ifFalse,expectedTypeInfo = newExpectedType)})"
        }

        private fun CodeBuilder.rawTypeFrom(node: JsonNode, propertyName: String? = null, expectedType: ResourceTypeInfo, explicit: Boolean = false) =
            if(node.isObject){
                val (name, properties) = node.fields().next()
                when(name){
                    "Fn::Join" -> joinFrom(properties)
                    "Fn::If" -> ifFrom(properties, expectedType)
                    "Fn::FindInMap" -> findInMapFrom(properties)
                    "Fn::Base64" -> base64From(properties)
                    "Fn::GetAtt" -> attFrom(properties, expectedType)
                    "Ref" -> refFrom(properties.textValue(), expectedType, explicit)
                    "Fn::GetAZs" -> getAzsFrom(properties)
                    else -> "+\"UNKNOWN\""
                }
            } else {
                valueTypeFor(node, expectedType)
            }.let{
                if(propertyName != null && !expectedType.required) "$propertyName($it)" else it
            }

        private fun CodeBuilder.value(node: JsonNode, propertyName: String? = null, expectedTypeInfo: ResourceTypeInfo, explicit: Boolean = false): String {
            return if(expectedTypeInfo.valueType && expectedTypeInfo.parameterA != null)
                rawTypeFrom(node, propertyName, expectedTypeInfo.parameterA, explicit)
            else if(expectedTypeInfo.list){
                val start = if(propertyName != null && !expectedTypeInfo.required) "$propertyName(" else ""
                val end = if(propertyName != null && !expectedTypeInfo.required) ")" else ""
                (if(node.isArray) node.elementsAsList() else listOf(node)).accumulate("${start}listOf(\n%>", "%<)$end", ",\n") {
                    item -> value(item, expectedTypeInfo.parameterA?.className?.decapitalize(), expectedTypeInfo = expectedTypeInfo.parameterA!!, explicit = explicit)
                }
            } else if(expectedTypeInfo.className != null){
                createFunctionFrom(node, propertyName!!, expectedTypeInfo)
            }
            else rawTypeFrom(node, propertyName, expectedTypeInfo)
        }

        private fun CodeBuilder.requiredList(requiredList: Map<String, ResourceTypeInfo>, parentNode: JsonNode) =
            requiredList.accumulate { (propertyName, propertyType) ->
                parentNode.fieldsAsMap().similar(propertyName)?.let { propertyNode ->
                    "$propertyName = " + value(propertyNode, propertyName, propertyType)
                } ?: ""
            }

        private fun CodeBuilder.notRequiredList(notRequiredList: Map<String, ResourceTypeInfo>, parentNode: JsonNode) =
                notRequiredList.accumulate(separator = "\n") { (propertyName, propertyType) ->
                    parentNode.fieldsAsMap().similar(propertyName)?.let { propertyNode ->
                        value(propertyNode, propertyName, propertyType)
                    } ?: ""
                }

        private fun CodeBuilder.createFunctionFrom(properties: Map<String, JsonNode>, name: String, propertyType: ResourceTypeInfo): String =
            properties.similar(name)?.let { node -> value(node, name, propertyType) }  ?: ""

        private fun CodeBuilder.createFunctionFrom(node: JsonNode, name: String, propertyType: ResourceTypeInfo): String {
            val typeInfo = resourceInfo.similar(propertyType.rawType)!!
            staticImports.add(typeInfo.canonicalPackage to name)
            val requiredList = requiredList(typeInfo.required, node)
            val notRequiredList = notRequiredList(typeInfo.notRequired, node).let { if(it.isEmpty()) "\n" else "{%>$it%<}\n" }
            return "$name($requiredList)$notRequiredList"
        }

        private fun CodeBuilder.codeForParameters() = codeFrom(
                parameters.accumulate(separator = "\n") { (name, parameter) ->
                    val type = parameter.parameterType()
                    val typeString = if (type.first != "String") ", type = \"${type.first}\"" else ""
                    this + type.second
                    this + name
                    "val ${name.decapitalize()} = parameter<%T>(logicalName = %S$typeString)"
                } + if(parameters.isNotEmpty()) "\n" else ""
        )

        private fun CodeBuilder.codeForMappings(): CodeBlock = codeFrom(
                if(mappings.isNotEmpty()){
                    mappings.accumulate("mappings(\n%>", "\n%<)\n", separator = ",\n"){
                        (name, node) ->
                        this + name
                        "%S to " + node.fieldsAsMap().accumulate("mapOf(\n%>", "\n%<)", ",\n"){
                            (secondName, secondNode) ->
                            this + secondName
                            "%S to " + secondNode.fieldsAsMap().accumulate("mapOf(\n%>", "\n%<)", ",\n"){
                                (leafName, leafNode) ->
                                this + leafName
                                "%S to " + valueString(leafNode)
                            }
                        }
                    }
                }
                else ""
        )

        private fun codeForResources(): CodeBlock = reorder(
                resources.map{ (name, resource) ->
                val (_, typeInfo) = resource.resourceTypeInfo(name)
                val functionName = typeInfo.name.decapitalize()
                staticImports.add(typeInfo.canonicalPackage to functionName)
                val codeBuilder = CodeBuilder(refBuilder = RefBuilder(name = name))
                codeBuilder + name
                val required = typeInfo.required.accumulate("$functionName(logicalName = %S", ")", firstIncluded = true) {
                    (propertyName, propertyType) ->
                    "$propertyName = " + codeBuilder.createFunctionFrom(resource.properties(), propertyName, propertyType)
                }
                val notRequired = typeInfo.notRequired.accumulate(separator = "\n") {
                    (propertyName, propertyType) ->
                    codeBuilder.createFunctionFrom(resource.properties(), propertyName, propertyType)
                }
                codeBuilder.refBuilder = codeBuilder.refBuilder.copy(code = required + if(notRequired.isEmpty())"" else "{\n%>$notRequired\n%<}")
                codeBuilder
            }
        )

        private fun functionForTemplate(node: JsonNode): FunSpec {
            parameters.putAll(node.mapFromFieldNamed("Parameters"))
            mappings.putAll(node.mapFromFieldNamed("Mappings"))
            resources.putAll(node.mapFromFieldNamed("Resources"))
            return FunSpec.builder(functionName)
                    .returns(KloudFormationTemplate::class)
                    .addCode("return %T.create {\n%>%>", KloudFormationTemplate::class)
                    .addCode(CodeBuilder().codeForParameters())
                    .addCode(CodeBuilder().codeForMappings())
                    .addCode(codeForResources())
                    .addCode("\n%<}\n%<")
                    .build()
        }

        private fun classForTemplate(node: JsonNode) = TypeSpec.objectBuilder(className).addFunction(functionForTemplate(node)).build()

        private data class RefBuilder(
                val name: String = "",
                val code: String = "",
                val refs: MutableList<String> = mutableListOf()
        )

        private inner class CodeBuilder(val objectList: MutableList<Any> = mutableListOf(), var refBuilder: RefBuilder = RefBuilder()){
            operator fun plus(item: Any) = objectList.add(item)
            fun codeFrom(code: String): CodeBlock{
                return CodeBlock.of(code, *objectList.toTypedArray())
            }
        }

        fun reorder(codeBuilders: List<CodeBuilder>): CodeBlock{
            val codeBlocks = codeBuilders.toMutableList()
            val refCounts = codeBlocks.map { item -> item.refBuilder.name to codeBlocks.count { it.refBuilder.refs.contains(item.refBuilder.name) } }.toMap()
            codeBlocks.sortWith(Comparator { a, b ->
                val bDepsA = a.refBuilder.refs.contains(b.refBuilder.name)
                val aDepsB = b.refBuilder.refs.contains(a.refBuilder.name)
                if(bDepsA && aDepsB || (!bDepsA && !aDepsB)) 0
                else if(bDepsA) 1 else -1
            })
            return CodeBlock.builder().also { builder ->
                codeBlocks.forEach{ codeBlock ->
                    val code = if(refCounts[codeBlock.refBuilder.name] == 0) codeBlock.refBuilder.code
                    else "val ${codeBlock.refBuilder.name.decapitalize()} = ${codeBlock.refBuilder.code}"
                    builder.add(code + "\n", *codeBlock.objectList.toTypedArray()) }
            }.build()
        }

        override fun deserialize(parser: JsonParser, context: DeserializationContext): FileSpec =
                FileSpec.builder("$kPackage.stack", className)
                        .addType(classForTemplate(parser.codec.readTree(parser)))
                        .also { file -> staticImports.forEach { (pkg, name) -> file.addStaticImport(pkg, name) } }
                        .build()
    }
}