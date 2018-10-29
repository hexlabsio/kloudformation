package io.kloudformation

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
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
            ObjectMapper(YAMLFactory())
            .registerModule(SimpleModule().addDeserializer(FileSpec::class.java, StackInverter()))
            .readValue(template)

    private const val kPackage = "io.kloudformation"
    private const val className = "MyStack"
    private const val functionName = "stack"
    private val resourceInfo = resourceInfo()

    private fun String.variableName() = replace(Regex("[^a-zA-Z0-9]"),"").decapitalize()

    private fun String.propertyInfo(required: Boolean): ResourceTypeInfo{
        val isValue = startsWith("$kPackage.Value")
        val isList = startsWith("List") || startsWith("kotlin.collections.List")
        val isMap = startsWith("kotlin.collections.Map")
        val canonicalPackage = if(startsWith("AWS::") || equals("Tag")) canonicalPackageFor(this, false) else null
        val className = if(startsWith("AWS::")) substringAfterLast(".") else if(equals("Tag")) this else null
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
        if(awsType == "Tag") return "$kPackage.property"
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
        conversion(item).let { text -> "$acc${ if((index>0 || firstIncluded) && text.isNotEmpty() && acc.isNotEmpty()) separator else "" }$text" }
    } + end

    class StackInverter(
            private val staticImports: MutableList<Pair<String, String>> = mutableListOf(),
            private val parameters: MutableMap<String, JsonNode> = mutableMapOf(),
            private val conditions: MutableMap<String, JsonNode> = mutableMapOf(),
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
                else -> String::class
            }
        }

        private fun JsonNode.properties() = this["Properties"]?.fieldsAsMap() ?: emptyMap()

        private fun JsonNode.resourceTypeInfo(name: String) = this["Type"]?.textValue()?.let {
            it to ( resourceInfo[it] ?: throw InverterException("Did not have enough information to discover type $it"))
        } ?: throw IllegalArgumentException("Could not read type of resource with logical name $name")

        private fun CodeBuilder.valueTypeFor(value: JsonNode, expectedType: ResourceTypeInfo) =
            when {
                expectedType.list -> {
                    val items = if (value.isArray) value.elementsAsList() else listOf(value)
                    items.accumulate("+listOf(", ")") {
                        value(it, expectedTypeInfo = expectedType.parameterA!!)
                    }
                }
                expectedType.map -> {
                    value.fieldsAsMap().accumulate("+mapOf(\n%>", "\n%<", ",\n") { (name, node) ->
                        this + name
                        "%S to " + value(node, expectedTypeInfo = expectedType.parameterB!!)
                    }
                }
                else -> when (expectedType.rawType) {
                    "kotlin.String" -> {
                        this + value.asText(); "+%S"
                    }
                    else -> {
                        this + io.kloudformation.Value.Of::class
                        "%T(${value.asText()})"
                    }
                }
            }

        private fun CodeBuilder.valueString(item: JsonNode, explicit: Boolean = false) = value(item, explicit = explicit, expectedTypeInfo = ResourceTypeInfo(valueType = true, parameterA = ResourceTypeInfo("kotlin.String")))
        private fun CodeBuilder.valueBoolean(item: JsonNode, explicit: Boolean = false) = value(item, explicit = explicit, expectedTypeInfo = ResourceTypeInfo(valueType = true, parameterA = ResourceTypeInfo("kotlin.Boolean")))

        private fun CodeBuilder.attFrom(node: JsonNode, expectedType: ResourceTypeInfo): String {
            val (resource, attribute) = node.elementsAsList()
            val resourceText = resource.textValue()
            this + Att::class
            val attResource = if((parameters.keys + resources.keys).contains(resourceText)){
                refBuilder.refs += resourceText
                resourceText.variableName() + ".logicalName"
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
                val joinItems = items.elementsAsList()
                val separator = if(joinItems.size > 4) " +\n" else " + "
                joinItems.map { valueString(it, true) }.accumulate(separator = separator) { it }
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
                if(expectedType.rawType == "kotlin.String")  "${refItem.variableName()}.ref()"
                else {
                    this + Reference::class
                    val expectedRawType = if(expectedType.rawType == "kotlin.String") "String" else expectedType.rawType
                    "%T${if(explicit) "<$expectedRawType>" else ""}(${refItem.variableName()}.logicalName)"
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

        private fun CodeBuilder.importValueFrom(node: JsonNode): String {
            this + ImportValue::class
            return "%T(${valueString(node)})"
        }

        private fun CodeBuilder.findInMapFrom(node: JsonNode): String {
            val (map, top, second) = node.elementsAsList()
            this + FindInMap::class
            return "%T(${valueString(map)}, ${valueString(top)}, ${valueString(second)})"
        }

        private fun CodeBuilder.splitFrom(node: JsonNode): String {
            val (delimiter, sourceString) = node.elementsAsList()
            this + Split::class
            this + delimiter
            return "%T(%S, ${valueString(sourceString)})"
        }

        private fun CodeBuilder.subFrom(node: JsonNode, expectedType: ResourceTypeInfo): String =
             if(node.isArray){
                val (string, variables) = node.elementsAsList()
                this + Sub::class
                this + string
                val newExpectedType = "kotlin.collections.Map<$kPackage.Value<${expectedType.rawType}>>".propertyInfo(expectedType.required)
                 "%T(%S, ${value(variables, expectedTypeInfo = newExpectedType)})"
            } else{
                this + Sub::class
                this + node.textValue()
                 "%T(%S)"
            }

        private fun CodeBuilder.ifFrom(node: JsonNode, expectedType: ResourceTypeInfo): String {
            val (condition, ifTrue, ifFalse) = node.elementsAsList()
            this + If::class
            this + condition.textValue()
            val newExpectedType = "$kPackage.Value<${expectedType.rawType}>".propertyInfo(expectedType.required)
            return "%T(%S, ${value(ifTrue,expectedTypeInfo = newExpectedType)}, ${value(ifFalse,expectedTypeInfo = newExpectedType)})"
        }

        private fun CodeBuilder.selectFrom(node: JsonNode, expectedType: ResourceTypeInfo): String {
            val (index, items) = node.elementsAsList()
            this + Select::class
            val newExpectedType = "kotlin.collections.List<$kPackage.Value<${expectedType.rawType}>>".propertyInfo(expectedType.required)
            return "%T(${valueString(index)}, ${value(items,expectedTypeInfo = newExpectedType)})"
        }

        private fun CodeBuilder.andOrFrom(node: JsonNode, and: Boolean): String {
            val (a, b) = node.elementsAsList()
            val name = if(and) "and" else "or"
            staticImports += "$kPackage.function" to name
            return "(${valueBoolean(a)} $name ${valueBoolean(b)})"
        }

        private fun CodeBuilder.equalsFrom(node: JsonNode): String {
            val (a, b) = node.elementsAsList()
            staticImports += "$kPackage.function" to "eq"
            return "(${valueString(a)} eq ${valueString(b)})" //TODO Check that value string is ok here, can by anything
        }

        private fun CodeBuilder.notFrom(node: JsonNode): String {
            val (value) = node.elementsAsList()
            staticImports += "$kPackage.function" to "not"
            return "not(${valueBoolean(value)})"
        }

        private fun CodeBuilder.conditionFrom(node: JsonNode): String {
            val conditionName = node.textValue()
            return if(conditions.containsKey(conditionName)) conditionName.variableName()
            else{
                this + Condition::class
                this + conditionName
                "%T(%S)"
            }
        }

        private fun CodeBuilder.rawTypeFrom(node: JsonNode, propertyName: String? = null, expectedType: ResourceTypeInfo, explicit: Boolean = false) =
            if(node.isObject){
                val (name, properties) = node.fields().next()
                when (name) {
                    "Fn::Join" -> joinFrom(properties)
                    "Fn::Select" -> selectFrom(properties, expectedType)
                    "Fn::If" -> ifFrom(properties, expectedType)
                    "Fn::And" -> andOrFrom(properties, true)
                    "Fn::Equals" -> equalsFrom(properties)
                    "Fn::Not" -> notFrom(properties)
                    "Fn::Or" -> andOrFrom(properties, false)
                    "Fn::FindInMap" -> findInMapFrom(properties)
                    "Fn::Base64" -> base64From(properties)
                    "Fn::GetAtt" -> attFrom(properties, expectedType)
                    "Ref" -> refFrom(properties.textValue(), expectedType, explicit)
                    "Fn::GetAZs" -> getAzsFrom(properties)
                    "Fn::ImportValue" -> importValueFrom(properties)
                    "Fn::Split" -> splitFrom(properties)
                    "Fn::Sub" -> subFrom(properties, expectedType)
                    "Condition" -> conditionFrom(properties)
                    else -> "+\"UNKNOWN\""
                }
            } else {
                valueTypeFor(node, expectedType)
            }.let{
                if(propertyName != null && !expectedType.required) "$propertyName($it)" else it
            }

        private fun CodeBuilder.actionsFrom(node: JsonNode, positive: Boolean): String{
            val value = if(node.isTextual && node.asText() == "*") {
                if(positive){
                    staticImports += "$kPackage.model.iam" to "allActions"
                    "allActions"
                } else {
                    staticImports += "$kPackage.model.iam" to "noActions"
                    "noActions"
                }
            }
            else{
                val items = (if(node.isArray) node.elementsAsList() else listOf(node))
                if(items.size == 1) {
                    val name = if(positive) "action" else "notAction"
                    staticImports += "$kPackage.model.iam" to name
                    "$name(\"${items.first().textValue()}\")"
                }
                else {
                    val name = if(positive) "actions" else "notActions"
                    staticImports += "$kPackage.model.iam" to name
                    "$name(${items.accumulate { "\"" + it.textValue() + "\"" }})"
                }
            }
            return "action = $value"
        }
        private fun CodeBuilder.resourcesFrom(node: JsonNode, positive: Boolean): String{
            val value = if(node.isTextual && node.asText() == "*") {
                if(positive){
                    staticImports += "$kPackage.model.iam" to "allResources"
                    "allResources"
                } else {
                    staticImports += "$kPackage.model.iam" to "noResources"
                    "noResources"
                }
            }
            else{
                val items = (if(node.isArray) node.elementsAsList() else listOf(node))
                if(items.size == 1) {
                    val name = if(positive) "resource" else "notResource"
                    staticImports += "$kPackage.model.iam" to name
                    "$name(${valueString(items.first())})"
                }
                else {
                    val name = if(positive) "resources" else "notResources"
                    staticImports += "$kPackage.model.iam" to name
                    "$name(${items.accumulate { valueString(items.first()) }})"
                }
            }
            return "resource = $value"
        }
        private fun CodeBuilder.principalFrom(node: JsonNode, positive: Boolean): String{
            return if(node.isTextual && node.asText() == "*") {
                if(positive){
                    staticImports += "$kPackage.model.iam" to "allPrincipals"
                    "allPrincipals"
                } else {
                    staticImports += "$kPackage.model.iam" to "noPrincipals"
                    "noPrincipals"
                }
            }
            else{
                val (principalType, principalNodes)  = node.fields().next()
                val principals = if(principalNodes.isArray) principalNodes.elementsAsList() else listOf(principalNodes)
                "${if(!positive)"notP" else "p"}rincipal(PrincipalType.${principalType.toUpperCase()}, listOf(${principals.accumulate { valueString(it) }}))"
            }
        }

        private fun CodeBuilder.statementFrom(node: JsonNode): String{
            val parameters = listOfNotNull(
                    node["Sid"]?.textValue()?.let { "sid = \"$it\"" },
                    (node["Action"])?.let { actionsFrom(it, true) },
                    (node["NotAction"])?.let { actionsFrom(it, false) },
                    (node["Resource"])?.let { resourcesFrom(it, true) },
                    (node["NotResource"])?.let { resourcesFrom(it, false) },
                    node["Effect"]?.textValue()?.let { "effect = IamPolicyEffect.$it" }
            ).accumulate()
            val body = listOfNotNull(
                    node["Principal"]?.let { principalFrom(it, true) },
                    node["NotPrincipal"]?.let { principalFrom(it, false) }
                    //TODO Conditions
            ).accumulate(separator = "\n")
            return "statement($parameters){\n%>$body%<\n}\n"
        }

        private fun CodeBuilder.policyDocFrom(node: JsonNode): String{
            listOf("IamPolicyEffect", "PrincipalType", "policyDocument").forEach{
                staticImports.add("$kPackage.model.iam" to it)
            }
            val parameters = listOfNotNull(
                    node["Id"]?.textValue()?.let { "id = \"$it\"" },
                    node["Version"]?.textValue()?.let { "version = \"$it\"" }
            ).accumulate()
            val statements = node["Statement"]?.let {  if(it.isArray) it.elementsAsList() else listOf(it) }.orEmpty()
            val body = statements.accumulate(separator = "\n"){ statementFrom(it) }
            return "policyDocument($parameters){\n%>$body%<}\n"
        }

        private fun isValueJsonNode(expectedType: ResourceTypeInfo) = expectedType.valueType && expectedType.parameterA?.rawType?.contains("JsonNode") == true


        private fun CodeBuilder.value(node: JsonNode, propertyName: String? = null, expectedTypeInfo: ResourceTypeInfo, explicit: Boolean = false): String {
            return if(expectedTypeInfo.valueType && expectedTypeInfo.parameterA != null) {
                if(isValueJsonNode(expectedTypeInfo)) policyDocFrom(node)
                else rawTypeFrom(node, propertyName, expectedTypeInfo.parameterA, explicit)
            }
            else if(expectedTypeInfo.list){
                val start = if(propertyName != null && !expectedTypeInfo.required) "$propertyName(" else ""
                val end = if(propertyName != null && !expectedTypeInfo.required) ")" else ""
                (if(node.isArray) node.elementsAsList() else listOf(node)).accumulate("${start}listOf(\n%>", "%<)$end", ",\n") {
                    item -> value(item, expectedTypeInfo.parameterA?.className?.decapitalize(), expectedTypeInfo = expectedTypeInfo.parameterA!!, explicit = explicit)
                }
            } else if(expectedTypeInfo.map){
                val start = if(propertyName != null && !expectedTypeInfo.required) "$propertyName(" else ""
                val end = if(propertyName != null && !expectedTypeInfo.required) ")" else ""
                node.fieldsAsMap().accumulate("${start}mapOf(\n%>", "%<\n)$end", ",\n") {
                    (name, item) ->
                    this + name
                    "%S to " + value(item, expectedTypeInfo.parameterB?.className?.decapitalize(), expectedTypeInfo = expectedTypeInfo.parameterB!!, explicit = explicit)
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
            val typeInfo = resourceInfo.similar(propertyType.rawType) ?: throw InverterException("Could not find information for type ${propertyType.rawType}")
            staticImports.add(typeInfo.canonicalPackage to name)
            val requiredList = requiredList(typeInfo.required, node)
            val notRequiredList = notRequiredList(typeInfo.notRequired, node).let { if (it.isEmpty()) "\n" else "{\n%>$it\n%<}\n" }
            return "$name($requiredList)$notRequiredList"
        }

        fun CodeBuilder.codeForParameters() = codeFrom(
                parameters.accumulate(separator = "\n") { (name, parameter) ->
                    val type = parameter.parameterType()
                    val typeString = if (type.first != "String") ", type = \"${type.first}\"" else ""
                    this + type.second
                    this + name
                    "val ${name.variableName()} = parameter<%T>(logicalName = %S$typeString)"
                } + if(parameters.isNotEmpty()) "\n" else ""
        )

        fun CodeBuilder.codeForConditions() = codeFrom(
                conditions.accumulate(separator = "\n") { (name, condition) ->
                    this + name
                    "val ${name.variableName()} = condition(%S, ${valueBoolean(condition)})"
                } + if(conditions.isNotEmpty()) "\n" else ""
        )

        fun CodeBuilder.codeForMappings(): CodeBlock = codeFrom(
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

        fun CodeBuilder.conditionReferenceFor(condition: String): String{
            return if(conditions.containsKey(condition)) condition.variableName() + ".logicalName"
            else {
                this + condition
                "%S"
            }
        }

        fun codeForResources(): CodeBlock = reorder(
                resources.map{ (name, resource) ->
                val (_, typeInfo) = resource.resourceTypeInfo(name)
                val functionName = typeInfo.name.decapitalize()
                staticImports.add(typeInfo.canonicalPackage to functionName)
                val codeBuilder = CodeBuilder(refBuilder = RefBuilder(name = name))
                codeBuilder + name
                val fields = listOfNotNull(
                        "logicalName" to "%S",
                        resource["Condition"]?.let { "condition" to codeBuilder.conditionReferenceFor(it.textValue()) }
                ).accumulate { (key, value) -> "$key = $value"}
                val required = typeInfo.required.accumulate("$functionName($fields", ")", firstIncluded = true) {
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
            conditions.putAll(node.mapFromFieldNamed("Conditions"))
            mappings.putAll(node.mapFromFieldNamed("Mappings"))
            resources.putAll(node.mapFromFieldNamed("Resources"))
            return FunSpec.builder(functionName)
                    .returns(KloudFormationTemplate::class)
                    .addCode("return %T.create {\n%>%>", KloudFormationTemplate::class)
                    .addCode(CodeBuilder().codeForParameters())
                    .addCode(CodeBuilder().codeForConditions())
                    .addCode(CodeBuilder().codeForMappings())
                    .addCode(codeForResources())
                    .addCode("\n%<}\n%<")
                    .build()
        }

        private fun classForTemplate(node: JsonNode) = TypeSpec.objectBuilder(className).addFunction(functionForTemplate(node)).build()

        data class RefBuilder(
                val name: String = "",
                val code: String = "",
                val refs: MutableList<String> = mutableListOf()
        )

        inner class CodeBuilder(val objectList: MutableList<Any> = mutableListOf(), var refBuilder: RefBuilder = RefBuilder()){
            operator fun plus(item: Any) = objectList.add(item)
            fun codeFrom(code: String): CodeBlock{
                return CodeBlock.of(code, *objectList.toTypedArray())
            }
        }

        private fun reorder(codeBuilders: List<CodeBuilder>): CodeBlock{
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
                    else "val ${codeBlock.refBuilder.name.variableName()} = ${codeBlock.refBuilder.code}"
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