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
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import io.kloudformation.model.KloudFormationTemplate
import io.kloudformation.model.Output
import io.kloudformation.specification.SpecificationPoet
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.AbstractConstruct
import org.yaml.snakeyaml.constructor.Constructor
import java.io.File
import java.lang.IllegalArgumentException
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import io.kloudformation.function.Att
import io.kloudformation.function.Condition
import io.kloudformation.function.FindInMap
import io.kloudformation.function.FnBase64
import io.kloudformation.function.GetAZs
import io.kloudformation.function.If
import io.kloudformation.function.ImportValue
import io.kloudformation.function.Join
import io.kloudformation.function.Reference
import io.kloudformation.function.Select
import io.kloudformation.function.Split
import io.kloudformation.function.Sub
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.SequenceNode
import org.yaml.snakeyaml.nodes.Tag
import java.nio.file.Path

fun main(args: Array<String>) {
    try {
        val fileText = File(args[0]).readText()
        Inverter.invert(fileText).writeTo(File(args[1]))
    } catch (e: Exception) {
        if (e.message != null) error(e.message.toString()) else e.printStackTrace()
    }
}


object AwsConstructor : Constructor() {
    private val arrayNodes = listOf("Cidr", "And", "Equals", "If", "Not", "Or", "FindInMap", "Join", "Select", "Split", "Sub")
    private val objectNodes = listOf("Base64", "GetAZs", "ImportValue", "Transform")
    private val mappings = arrayNodes.map { it to { node: Node -> mapOf("Fn::$it" to node.arrayValue()) } }.toMap() +
            objectNodes.map { it to { node: Node -> mapOf("Fn::$it" to node.objectValue()) } }.toMap() +
            ("Ref" to { node -> mapOf("Ref" to node.stringValue()) }) +
            ("GetAtt" to { node -> mapOf("Fn::GetAtt" to node.attributeValue()) })

    private fun Node.attributeValue() = if (this is ScalarNode) {
        val firstComponent = this.value.substringBefore(".")
        val otherComponents = this.value.substringAfter(".")
        listOf(firstComponent, otherComponents)
    } else arrayValue()
    private fun Node.stringValue() = (this as ScalarNode).value
    private fun Node.arrayValue(): Any = when (this) {
        is SequenceNode -> constructSequence(this)
        else -> this.stringValue()
    }
    private fun Node.objectValue(): Any = when (this) {
        is MappingNode -> constructMapping(this)
        else -> this.stringValue()
    }

    init {
        mappings.forEach { tag, construct ->
            this.yamlConstructors[Tag("!$tag")] = object : AbstractConstruct() {
                override fun construct(node: Node): Any = construct(node)
            }
        }
    }
}
data class Inversion( val fileSpec: FileSpec) {
    fun writeTo(dir: Path) = fileSpec.writeTo(dir)
    fun writeTo(dir: File) = fileSpec.writeTo(dir)
    fun writeTo(dir: Appendable) = fileSpec.writeTo(dir)
    override fun toString() = fileSpec.toString()
}
object Inverter {
    class InverterException(message: String) : Exception(message)
    data class ResourceInfo(val type: String, val canonicalPackage: String, val name: String, val required: Map<String, ResourceTypeInfo>, val notRequired: Map<String, ResourceTypeInfo>)
    data class ResourceTypeInfo(val rawType: String = "", val canonicalPackage: String? = null, val className: String? = null, val required: Boolean = true, val valueType: Boolean = false, val list: Boolean = false, val map: Boolean = false, val parameterA: ResourceTypeInfo? = null, val parameterB: ResourceTypeInfo? = null)

    fun mapToStandard(yaml: String): String = with(Yaml(AwsConstructor)) { dump(load(yaml)).trim() }

    fun invert(template: String): Inversion {
        return mapToStandard(template)
                .let{ ObjectMapper(YAMLFactory())
                        .registerModule(SimpleModule().addDeserializer(FileSpec::class.java, StackInverter()))
                        .readValue<FileSpec>(it) }
                .let{ Inversion(it) }



    }

    private val escapees = mapOf(
            "\\" to "\\\\",
            "\b" to "\\b",
            "\n" to "\\n",
            "\t" to "\\t",
            "\r" to "\\r",
            "\"" to "\\",
            "$" to "\${'$'}",
            " " to "·"
    )

    fun escape(value: String): String = value.let {
        escapees.entries.fold(it) { string, (escapee, escaped) ->
            string.replace(escapee, escaped)
        }
    }

    private const val kPackage = "io.kloudformation"
    private const val className = "MyStack"
    private const val functionName = "stack"
    private val resourceInfo = resourceInfo()

    private fun String.variableName() = replace(Regex("[^a-zA-Z0-9]"), "").decapitalize()

    private fun String.propertyInfo(required: Boolean): ResourceTypeInfo {
        val isValue = startsWith("$kPackage.Value")
        val isList = startsWith("List") || startsWith("kotlin.collections.List")
        val isMap = startsWith("kotlin.collections.Map")
        val canonicalPackage = if (startsWith("AWS::") || equals("Tag")) canonicalPackageFor(this, false) else null
        val className = if (startsWith("AWS::")) substringAfterLast(".") else if (equals("Tag")) this else null
        val parameterA = when {
            isList || isValue -> substringAfter("<").substringBeforeLast(">")
            isMap -> substringAfter("<").substringBefore(",")
            else -> null
        }?.propertyInfo(required)
        val parameterB = when {
            isMap -> substringAfter(",").substringBeforeLast(">")
            else -> null
        }?.propertyInfo(required)
        return ResourceTypeInfo(this, canonicalPackage, className, required, isValue, isList, isMap, parameterA, parameterB)
    }

    private fun resourceInfo(): Map<String, ResourceInfo> {
        fun Map<String, String>.info(required: Boolean) = map { (name, type) -> name to type.propertyInfo(required) }.toMap()
        return jacksonObjectMapper().readValue<Map<String, SpecificationPoet.Info>>(Inverter::class.java.classLoader.getResource("info.json"))
                .map { (awsType, info) ->
                    awsType to ResourceInfo(awsType, canonicalPackageFor(awsType, info.resource), info.name, info.required.info(true), info.notRequired.info(false))
                }.toMap()
    }

    private fun canonicalPackageFor(awsType: String, resource: Boolean): String {
        if (awsType == "Tag") return "$kPackage.property"
        val resourcePackage = awsType.substringAfter("::").substringBefore("::").toLowerCase()
        val otherPackages = awsType.substringAfter("::").substringAfter("::").split(".")
        val otherPackagesString = otherPackages.subList(0, otherPackages.size - 1).accumulate(separator = ".") { it.toLowerCase() }
        val tail = if (otherPackagesString.isNotEmpty()) ".$otherPackagesString" else ""
        return "$kPackage.${if (resource) "resource" else "property"}.aws.$resourcePackage$tail"
    }

    private fun <A, S> Map<A, S>.accumulate(start: String = "", end: String = "", separator: String = ", ", firstIncluded: Boolean = false, conversion: (Map.Entry<A, S>) -> String) =
            asSequence().toList().accumulate(start, end, separator, firstIncluded, conversion)

    private fun <S> List<S>.accumulate(
        start: String = "",
        end: String = "",
        separator: String = ", ",
        firstIncluded: Boolean = false,
        conversion: (S) -> String = { "$it" }
    ): String = foldIndexed(start) { index, acc, item ->
        conversion(item).let { text -> "$acc${if ((index > 0 || firstIncluded) && text.isNotEmpty() && acc.isNotEmpty()) separator else ""}$text" }
    } + end

    class StackInverter(
        private val staticImports: MutableList<Pair<String, String>> = mutableListOf(),
        private val parameters: MutableMap<String, JsonNode> = mutableMapOf(),
        private val conditions: MutableMap<String, JsonNode> = mutableMapOf(),
        private val mappings: MutableMap<String, JsonNode> = mutableMapOf(),
        private val resources: MutableMap<String, JsonNode> = mutableMapOf()
    ) : StdDeserializer<FileSpec>(FileSpec::class.java) {

        private fun <T> Map<String, T>.similar(key: String): T? = keys.find { it.equals(key, true) }?.let { this[it] }
        private fun JsonNode.elementsAsList() = elements().asSequence().toList()
        private fun JsonNode.fieldsAsList() = fields().asSequence().toList()
        private fun JsonNode.fieldsAsMap() = fields().asSequence().toList().map { (key, value) -> key to value }.toMap()
        private fun JsonNode.mapFromFieldNamed(fieldName: String) = this[fieldName]?.fieldsAsList()?.map { it.key to it.value }?.toMap()
                ?: emptyMap()

        private fun JsonNode.parameterType() = with(this["Type"]?.textValue() ?: "String") {
            this to when (this) {
                "List<Number>" -> List::class.asClassName().parameterizedBy(String::class.asClassName())
                "List<String>" -> List::class.asClassName().parameterizedBy(String::class.asClassName())
                "CommaDelimitedList" -> List::class.asClassName().parameterizedBy(String::class.asClassName())
                else -> if (startsWith("List")) List::class.asClassName().parameterizedBy(String::class.asClassName()) else String::class
            }
        }

        private fun JsonNode.properties() = this["Properties"]?.fieldsAsMap() ?: emptyMap()

        private fun JsonNode.resourceTypeInfo(name: String) = this["Type"]?.textValue()?.let { custom ->
            val customResource = custom.startsWith("Custom::")
            val typeName = if (customResource) "AWS::CloudFormation::CustomResource" else custom
            custom to (resourceInfo[typeName]?.let { if (customResource) it.copy(type = custom) else it }
                    ?: throw InverterException("Did not have enough information to discover type $custom"))
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
                        value.fieldsAsMap().accumulate("+mapOf(\n⇥", "\n⇤", ",\n") { (name, node) ->
                            this + name
                            "%S to " + value(node, expectedTypeInfo = expectedType.parameterB!!)
                        }
                    }
                    else -> when (expectedType.rawType) {
                        "kotlin.String" -> {
                            "+\"" + escape(value.asText()) + "\""
                        }
                        else -> {
                            this + io.kloudformation.Value.Of::class
                            "%T(${value.asText()})"
                        }
                    }
                }

        private fun CodeBuilder.valueString(item: JsonNode, explicit: Boolean = false) = value(item, explicit = explicit, expectedTypeInfo = ResourceTypeInfo(valueType = true, parameterA = ResourceTypeInfo("kotlin.String")))
        private fun CodeBuilder.valueListString(item: JsonNode, explicit: Boolean = false) = value(item, explicit = explicit, expectedTypeInfo = ResourceTypeInfo(valueType = true, parameterA = ResourceTypeInfo("kotlin.collections.List", list = true, parameterA = ResourceTypeInfo("kotlin.String"))))
        private fun CodeBuilder.valueBoolean(item: JsonNode, explicit: Boolean = false) = value(item, explicit = explicit, expectedTypeInfo = ResourceTypeInfo(valueType = true, parameterA = ResourceTypeInfo("kotlin.Boolean")))
        private fun CodeBuilder.valueInt(item: JsonNode, explicit: Boolean = false) = value(item, explicit = explicit, expectedTypeInfo = ResourceTypeInfo(valueType = true, parameterA = ResourceTypeInfo("kotlin.Int")))

        private fun CodeBuilder.attFrom(node: JsonNode, expectedType: ResourceTypeInfo): String {
            val (resource, attribute) = node.elementsAsList()
            val resourceText = resource.textValue()
            this + Att::class
            val attResource = if ((parameters.keys + resources.keys).contains(resourceText)) {
                refBuilder.refs += resourceText
                resourceText.variableName() + ".logicalName"
            } else "\"${escape(resourceText)}\""
            return "%T<${expectedType.rawType}>($attResource, " + valueString(attribute) + ")"
        }

        private fun CodeBuilder.joinFrom(node: JsonNode): String {
            val (splitter, items) = node.elementsAsList()
            return if (splitter.asText().isEmpty()) {
                staticImports.add("$kPackage.function" to "plus")
                val joinItems = items.elementsAsList()
                val separator = if (joinItems.size > 4) " +\n" else " + "
                joinItems.map { valueString(it, true) }.accumulate(separator = separator) { it }
            } else {
                this + Join::class
                this + splitter.asText()
                "%T(%S, ${items.elementsAsList().map { valueString(it) }.accumulate("listOf(\n⇥", "\n⇤)", separator = ", \n") { it }})"
            }
        }

        private fun CodeBuilder.base64From(node: JsonNode): String {
            this + FnBase64::class
            return "%T(" + valueString(node) + ")"
        }

        private fun CodeBuilder.refFrom(refItem: String, expectedType: ResourceTypeInfo, explicit: Boolean = false) =
                if ((parameters.keys + resources.keys).contains(refItem)) {
                    refBuilder.refs += refItem
                    if (expectedType.rawType == "kotlin.String") "${refItem.variableName()}.ref()"
                    else {
                        this + Reference::class
                        val expectedRawType = if (expectedType.rawType == "kotlin.String") "String" else expectedType.rawType
                        "%T${if (explicit) "<$expectedRawType>" else ""}(${refItem.variableName()}.logicalName)"
                    }
                } else {
                    val special = when (refItem) {
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
                    if (special != null) {
                        staticImports.add("io.kloudformation.model.KloudFormationTemplate.Builder.Companion" to special.substringBeforeLast("()"))
                        special
                    } else {
                        this + Reference::class
                        "%T${if (explicit) "<${expectedType.rawType}>" else ""}(\"${escape(refItem)}\")"
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
            java.io.File("bob").writeText("Hello")
            println(java.io.File("bob").readText())
            return "%T(%S, ${valueString(sourceString)})"
        }

        private fun CodeBuilder.subFrom(node: JsonNode, expectedType: ResourceTypeInfo): String =
                if (node.isArray) {
                    val (string, variables) = node.elementsAsList()
                    this + Sub::class
                    this + string
                    val newExpectedType = "kotlin.collections.Map<$kPackage.Value<${expectedType.rawType}>>".propertyInfo(expectedType.required)
                    "%T(%S, ${value(variables, expectedTypeInfo = newExpectedType)})"
                } else {
                    this + Sub::class
                    "%T(\"${escape(node.textValue())}\")"
                }

        private fun CodeBuilder.ifFrom(node: JsonNode, expectedType: ResourceTypeInfo): String {
            val (condition, ifTrue, ifFalse) = node.elementsAsList()
            this + If::class
            val conditionReference = conditionReferenceFor(condition.textValue())
            val newExpectedType = "$kPackage.Value<${expectedType.rawType}>".propertyInfo(expectedType.required)
            return "%T($conditionReference, ${value(ifTrue, expectedTypeInfo = newExpectedType)}, ${value(ifFalse, expectedTypeInfo = newExpectedType)})"
        }

        private fun CodeBuilder.selectFrom(node: JsonNode, expectedType: ResourceTypeInfo): String {
            val (index, items) = node.elementsAsList()
            this + Select::class
            val newExpectedType = "kotlin.collections.List<$kPackage.Value<${expectedType.rawType}>>".propertyInfo(expectedType.required)
            return "%T(${valueString(index)}, ${value(items, expectedTypeInfo = newExpectedType)})"
        }

        private fun CodeBuilder.andOrFrom(node: JsonNode, and: Boolean): String {
            val (a, b) = node.elementsAsList()
            val name = if (and) "and" else "or"
            staticImports += "$kPackage.function" to name
            return "(${valueBoolean(a)} $name ${valueBoolean(b)})"
        }

        private fun CodeBuilder.equalsFrom(node: JsonNode): String {
            val (a, b) = node.elementsAsList()
            staticImports += "$kPackage.function" to "eq"
            return "(${valueString(a)} eq ${valueString(b)})" // TODO Check that value string is ok here, can by anything
        }

        private fun CodeBuilder.notFrom(node: JsonNode): String {
            val (value) = node.elementsAsList()
            staticImports += "$kPackage.function" to "not"
            return "not(${valueBoolean(value)})"
        }

        private fun CodeBuilder.conditionFrom(node: JsonNode): String {
            val conditionName = node.textValue()
            return if (conditions.containsKey(conditionName)) conditionName.variableName()
            else {
                this + Condition::class
                this + conditionName
                "%T(%S)"
            }
        }

        private fun CodeBuilder.rawTypeFrom(node: JsonNode, propertyName: String? = null, expectedType: ResourceTypeInfo, explicit: Boolean = false) =
                if (node.isObject) {
                    val fields = node.fields()
                    if (fields.hasNext()) {
                        val (name, properties) = fields.next()
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
                        this + EmptyObject::class
                        "%T()"
                    }
                } else {
                    valueTypeFor(node, expectedType)
                }.let {
                    if (propertyName != null && !expectedType.required) "$propertyName($it)" else it
                }

        private fun CodeBuilder.actionsFrom(node: JsonNode, positive: Boolean): String {
            val value = if (node.isTextual && node.asText() == "*") {
                if (positive) {
                    staticImports += "$kPackage.model.iam" to "allActions"
                    "allActions"
                } else {
                    staticImports += "$kPackage.model.iam" to "noActions"
                    "noActions"
                }
            } else {
                val items = (if (node.isArray) node.elementsAsList() else listOf(node))
                if (items.size == 1) {
                    val name = if (positive) "action" else "notAction"
                    staticImports += "$kPackage.model.iam" to name
                    "$name(\"${items.first().textValue()}\")"
                } else {
                    val name = if (positive) "actions" else "notActions"
                    staticImports += "$kPackage.model.iam" to name
                    "$name(${items.accumulate { "\"" + escape(it.textValue()) + "\"" }})"
                }
            }
            return "action = $value"
        }

        private fun CodeBuilder.resourcesFrom(node: JsonNode, positive: Boolean): String {
            val value = if (node.isTextual && node.asText() == "*") {
                if (positive) {
                    staticImports += "$kPackage.model.iam" to "allResources"
                    "allResources"
                } else {
                    staticImports += "$kPackage.model.iam" to "noResources"
                    "noResources"
                }
            } else {
                val items = (if (node.isArray) node.elementsAsList() else listOf(node))
                if (items.size == 1) {
                    val name = if (positive) "resource" else "notResource"
                    staticImports += "$kPackage.model.iam" to name
                    "$name(${valueString(items.first())})"
                } else {
                    val name = if (positive) "resources" else "notResources"
                    staticImports += "$kPackage.model.iam" to name
                    "$name(${items.accumulate { valueString(items.first()) }})"
                }
            }
            return "resource = $value"
        }

        private fun CodeBuilder.principalFrom(node: JsonNode, positive: Boolean): String {
            return if (node.isTextual && node.asText() == "*") {
                if (positive) "allPrincipals()"
                else "noPrincipals()"
            } else {
                val (principalType, principalNodes) = node.fields().next()
                val principals = if (principalNodes.isArray) principalNodes.elementsAsList() else listOf(principalNodes)
                "${if (!positive) "notP" else "p"}rincipal(PrincipalType.${principalType.toUpperCase()}, listOf(${principals.accumulate { valueString(it) }}))"
            }
        }

        private fun CodeBuilder.iamConditionFrom(node: JsonNode): String {
            return node.fieldsAsMap().accumulate(separator = "\n") { (name, fieldNode) ->
                this + name
                val conditions = fieldNode.fieldsAsMap()
                val conditionString = conditions.accumulate { (key, conditionListNode) ->
                    val conditionList = (if (conditionListNode.isArray) conditionListNode.elementsAsList() else listOf(conditionListNode))
                            .accumulate { valueString(it) }
                    "\"$key\" to listOf($conditionList)"
                }
                "condition(%S, mapOf($conditionString))"
            }
        }

        private fun CodeBuilder.statementFrom(node: JsonNode): String {
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
                    node["NotPrincipal"]?.let { principalFrom(it, false) },
                    node["Condition"]?.let { iamConditionFrom(it) }
            ).accumulate(separator = "\n")
            return "statement($parameters)" + if (body.isNotEmpty()) "{\n⇥$body⇤\n}\n" else "\n"
        }

        private fun CodeBuilder.policyDocOrJsonFrom(propertyName: String?, expectedTypeInfo: ResourceTypeInfo, node: JsonNode): String {
            val value = if (node.fieldsAsMap().keys.contains("Statement")) {
                listOf("IamPolicyEffect", "PrincipalType", "policyDocument").forEach {
                    staticImports.add("$kPackage.model.iam" to it)
                }
                val parameters = listOfNotNull(
                        node["Id"]?.textValue()?.let { "id = \"$it\"" },
                        node["Version"]?.textValue()?.let { "version = \"$it\"" }
                ).accumulate()
                val statements = node["Statement"]?.let { if (it.isArray) it.elementsAsList() else listOf(it) }.orEmpty()
                val body = statements.accumulate(separator = "\n") { statementFrom(it) }
                "policyDocument($parameters){\n⇥$body⇤}\n"
            } else jsonFor(node)
            return if (propertyName != null && !expectedTypeInfo.required) "$propertyName($value)" else value
        }

        private fun isValueJsonNode(expectedType: ResourceTypeInfo) = expectedType.valueType && expectedType.parameterA?.rawType?.contains("JsonNode") == true

        private fun CodeBuilder.value(node: JsonNode, propertyName: String? = null, expectedTypeInfo: ResourceTypeInfo, explicit: Boolean = false): String {
            return if (expectedTypeInfo.valueType && expectedTypeInfo.parameterA != null) {
                if (isValueJsonNode(expectedTypeInfo)) policyDocOrJsonFrom(propertyName, expectedTypeInfo.parameterA, node)
                else rawTypeFrom(node, propertyName, expectedTypeInfo.parameterA, explicit)
            } else if (expectedTypeInfo.list) {
                val start = if (propertyName != null && !expectedTypeInfo.required) "$propertyName(" else ""
                val end = if (propertyName != null && !expectedTypeInfo.required) ")" else ""
                if (node.isArray) node.elementsAsList().accumulate("${start}listOf(\n⇥", "⇤)$end", ",\n") { item ->
                    value(item, expectedTypeInfo.parameterA?.className?.decapitalize(), expectedTypeInfo = expectedTypeInfo.parameterA!!, explicit = explicit)
                }
                else rawTypeFrom(node, propertyName, expectedTypeInfo, explicit)
            } else if (expectedTypeInfo.map) {
                val start = if (propertyName != null && !expectedTypeInfo.required) "$propertyName(" else ""
                val end = if (propertyName != null && !expectedTypeInfo.required) ")" else ""
                node.fieldsAsMap().accumulate("${start}mapOf(\n⇥", "⇤\n)$end", ",\n") { (name, item) ->
                    this + name
                    "%S to " + value(item, expectedTypeInfo.parameterB?.className?.decapitalize(), expectedTypeInfo = expectedTypeInfo.parameterB!!, explicit = explicit)
                }
            } else if (expectedTypeInfo.className != null) {
                createFunctionFrom(node, propertyName!!, expectedTypeInfo)
            } else rawTypeFrom(node, propertyName, expectedTypeInfo)
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
                properties.similar(name)?.let { node -> value(node, name, propertyType) } ?: ""

        private fun CodeBuilder.createFunctionFrom(node: JsonNode, name: String, propertyType: ResourceTypeInfo): String {
            val typeInfo = resourceInfo.similar(propertyType.rawType)
                    ?: throw InverterException("Could not find information for type ${propertyType.rawType}")
            val typeName = if (typeInfo.name == "Policy") {
                this + ClassName.bestGuess(typeInfo.canonicalPackage + "." + typeInfo.name)
                "%T"
            } else {
                staticImports.add(typeInfo.canonicalPackage to name)
                name
            }
            val requiredList = requiredList(typeInfo.required, node)
            val notRequiredList = notRequiredList(typeInfo.notRequired, node).let { if (it.isEmpty()) "\n" else "{\n⇥$it\n⇤}\n" }
            return "$typeName($requiredList)$notRequiredList"
        }

        fun CodeBuilder.codeForParameters() = codeFrom(
                parameters.accumulate(separator = "\n") { (name, parameter) ->
                    val (typeName, type) = parameter.parameterType()
                    val fields = listOfNotNull(
                            "logicalName = \"$name\"",
                            if (typeName != "String") "type = \"$typeName\"" else null,
                            parameter["AllowedPattern"]?.let { "allowedPattern = \"${escape(it.textValue())}\"" },
                            parameter["AllowedValues"]?.let { "allowedValues = listOf(${it.elementsAsList().accumulate()})" },
                            parameter["ConstraintDescription"]?.let { "constraintDescription = \"${escape(it.textValue())}\"" },
                            parameter["Default"]?.let { "default = \"${escape(it.textValue())}\"" },
                            parameter["Description"]?.let { "description = \"${escape(it.textValue())}\"" },
                            parameter["MaxLength"]?.let { "maxLength = \"${escape(it.textValue())}\"" },
                            parameter["MaxValue"]?.let { "maxValue = \"${escape(it.textValue())}\"" },
                            parameter["MinLength"]?.let { "minLength = \"${escape(it.textValue())}\"" },
                            parameter["MinValue"]?.let { "minValue = \"${escape(it.textValue())}\"" },
                            parameter["NoEcho"]?.let { "noEcho = \"${escape(it.textValue())}\"" }
                    ).accumulate()
                    this + type
                    "val ${name.variableName()} = parameter<%T>($fields)"
                } + if (parameters.isNotEmpty()) "\n" else ""
        )

        fun CodeBuilder.codeForConditions() = codeFrom(
                conditions.accumulate(separator = "\n") { (name, condition) ->
                    this + name
                    "val ${name.variableName()} = condition(%S, ${valueBoolean(condition)})"
                } + if (conditions.isNotEmpty()) "\n" else ""
        )

        fun CodeBuilder.codeForMappings(): CodeBlock = codeFrom(
                if (mappings.isNotEmpty()) {
                    mappings.accumulate("mappings(\n⇥", "\n⇤)\n", separator = ",\n") { (name, node) ->
                        this + name
                        "%S to " + node.fieldsAsMap().accumulate("mapOf(\n⇥", "\n⇤)", ",\n") { (secondName, secondNode) ->
                            this + secondName
                            "%S to " + secondNode.fieldsAsMap().accumulate("mapOf(\n⇥", "\n⇤)", ",\n") { (leafName, leafNode) ->
                                this + leafName
                                "%S to " + valueString(leafNode)
                            }
                        }
                    }
                } else ""
        )

        private fun CodeBuilder.conditionReferenceFor(condition: String): String {
            return if (conditions.containsKey(condition)) condition.variableName() + ".logicalName"
            else {
                this + condition
                "%S"
            }
        }

        private fun CodeBuilder.dependsOnFor(node: JsonNode): String =
                (if (node.isArray) node.elementsAsList() else listOf(node)).accumulate("listOf(", ")") {
                    val dependsOn = escape(it.textValue())
                    if (resources.containsKey(dependsOn)) {
                        refBuilder.refs += dependsOn
                        dependsOn.variableName() + ".logicalName"
                    } else "\"$dependsOn\""
                }

        private fun CodeBuilder.resourceSignalFrom(node: JsonNode): String {
            this + CreationPolicy.ResourceSignal::class
            val fields = listOfNotNull(
                    node["Count"]?.let { "count = " + valueInt(it) },
                    node["Timeout"]?.let { "timeout = " + valueString(it) }
            ).accumulate()
            return "%T($fields)"
        }

        private fun CodeBuilder.autoScalingCreationPolicyFrom(node: JsonNode): String {
            this + CreationPolicy.AutoScalingCreationPolicy::class
            val fields = listOfNotNull(
                    node["MinSuccessfulInstancesPercent"]?.let { "minSuccessfulInstancesPercent = " + valueInt(it) }
            ).accumulate()
            return "%T($fields)"
        }

        private fun CodeBuilder.autoScalingRollingUpdateFrom(node: JsonNode): String {
            this + AutoScalingRollingUpdate::class
            val fields = listOfNotNull(
                    node["MaxBatchSize"]?.let { "maxBatchSize = " + valueInt(it) },
                    node["MinInstancesInService"]?.let { "minInstancesInService = " + valueInt(it) },
                    node["MinSuccessfulInstancesPercent"]?.let { "minSuccessfulInstancesPercent = " + valueInt(it) },
                    node["PauseTime"]?.let { "pauseTime = " + valueString(it) },
                    node["SuspendProcesses"]?.let { "suspendProcesses = " + valueListString(it) },
                    node["WaitOnResourceSignals"]?.let { "waitOnResourceSignals = " + valueBoolean(it) }
            ).accumulate()
            return "%T($fields)"
        }

        private fun CodeBuilder.autoScalingReplacingUpdateFrom(node: JsonNode): String {
            this + AutoScalingReplacingUpdate::class
            val fields = listOfNotNull(
                    node["WillReplace"]?.let { "willReplace = " + valueBoolean(it) }
            ).accumulate()
            return "%T($fields)"
        }

        private fun CodeBuilder.autoScalingScheduledActionFrom(node: JsonNode): String {
            this + AutoScalingScheduledAction::class
            val fields = listOfNotNull(
                    node["IgnoreUnmodifiedGroupSizeProperties"]?.let { "ignoreUnmodifiedGroupSizeProperties = " + valueBoolean(it) }
            ).accumulate()
            return "%T($fields)"
        }

        private fun CodeBuilder.codeDeployLambdaAliasUpdateFrom(node: JsonNode): String {
            this + CodeDeployLambdaAliasUpdate::class
            val fields = listOfNotNull(
                    node["AfterAllowTrafficHook"]?.let { "afterAllowTrafficHook = " + valueString(it) },
                    node["ApplicationName"]?.let { "applicationName = " + valueString(it) },
                    node["BeforeAllowTrafficHook"]?.let { "beforeAllowTrafficHook = " + valueString(it) },
                    node["DeploymentGroupName"]?.let { "deploymentGroupName = " + valueString(it) }
            ).accumulate()
            return "%T($fields)"
        }

        private fun CodeBuilder.creationPolicyFor(node: JsonNode): String {
            this + CreationPolicy::class
            val fields = listOfNotNull(
                    node["AutoScalingCreationPolicy"]?.let { "autoScalingCreationPolicy = " + autoScalingCreationPolicyFrom(it) },
                    node["ResourceSignal"]?.let { "resourceSignal = " + resourceSignalFrom(it) }
            ).accumulate()
            return "%T($fields)"
        }

        private fun CodeBuilder.updatePolicyFor(node: JsonNode): String {
            this + UpdatePolicy::class
            val fields = listOfNotNull(
                    node["AutoScalingRollingUpdate"]?.let { "autoScalingRollingUpdate = " + autoScalingRollingUpdateFrom(it) },
                    node["AutoScalingReplacingUpdate"]?.let { "autoScalingReplacingUpdate = " + autoScalingReplacingUpdateFrom(it) },
                    node["AutoScalingScheduledAction"]?.let { "autoScalingScheduledAction = " + autoScalingScheduledActionFrom(it) },
                    node["CodeDeployLambdaAliasUpdate"]?.let { "codeDeployLambdaAliasUpdate = " + codeDeployLambdaAliasUpdateFrom(it) },
                    node["UseOnlineResharding"]?.let { "useOnlineResharding = " + valueBoolean(it) }
            ).accumulate()
            return "%T($fields)"
        }

        private fun CodeBuilder.deletionPolicyFor(node: JsonNode): String {
            staticImports += kPackage to "DeletionPolicy"
            return "DeletionPolicy." + node.textValue().toUpperCase() + ".policy"
        }

        fun CodeBuilder.jsonPartFor(node: JsonNode): String {
            return when {
                node.isObject -> {
                    val fields = node.fieldsAsMap()
                    fields.accumulate("mapOf(\n⇥", "\n⇤)") { (name, fieldNode) ->
                        this + name
                        "%S to ${jsonPartFor(fieldNode)}"
                    }
                }
                node.isArray -> {
                    val elements = node.elementsAsList()
                    elements.accumulate("listOf(\n⇥", "\n⇤)") { jsonPartFor(it) }
                }
                node.isTextual -> {
                    this + node.asText(); "%S"
                }
                else -> node.asText()
            }
        }

        fun CodeBuilder.jsonFor(node: JsonNode): String {
            staticImports += kPackage to "json"
            return "json(\n⇥${jsonPartFor(node)}\n⇤)"
        }

        fun codeForResources(refBuilder: RefBuilder = RefBuilder()): CodeBlock = reorder(
                resources.map { (name, resource) ->
                    val (_, typeInfo) = resource.resourceTypeInfo(name)
                    val functionName = typeInfo.name.decapitalize()
                    staticImports.add(typeInfo.canonicalPackage to functionName)
                    val codeBuilder = CodeBuilder(refBuilder = refBuilder.copy(name = name))
                    codeBuilder + name
                    val fields = listOfNotNull(
                            "logicalName" to "%S",
                            resource["DependsOn"]?.let { "dependsOn" to codeBuilder.dependsOnFor(it) },
                            "resourceProperties".let {
                                val parts = listOfNotNull(
                                        resource["Condition"]?.let { "condition" to codeBuilder.conditionReferenceFor(escape(it.textValue())) },
                                        resource["Metadata"]?.let { "metadata" to codeBuilder.jsonFor(it) },
                                        resource["CreationPolicy"]?.let { "creationPolicy" to codeBuilder.creationPolicyFor(it) },
                                        resource["UpdatePolicy"]?.let { "updatePolicy" to codeBuilder.updatePolicyFor(it) },
                                        resource["DeletionPolicy"]?.let { "deletionPolicy" to codeBuilder.deletionPolicyFor(it) }
                                )
                                if (parts.isNotEmpty()) {
                                    it to parts.accumulate(start = "ResourceProperties(", end = ")") { (key, value) -> "$key = $value" }
                                } else null
                            }

                    ).accumulate { (key, value) -> "$key = $value" }
                    val required = typeInfo.required.accumulate("$functionName($fields", ")", firstIncluded = true) { (propertyName, propertyType) ->
                        "$propertyName = " + codeBuilder.createFunctionFrom(resource.properties(), propertyName, propertyType)
                    }
                    val notRequired = typeInfo.notRequired.accumulate(separator = "\n") { (propertyName, propertyType) ->
                        codeBuilder.createFunctionFrom(resource.properties(), propertyName, propertyType)
                    }
                    val properties = resource.properties()
                    val isCustomCustomResource = typeInfo.type.startsWith("Custom::")
                    val totalPropertyList = typeInfo.required.keys + typeInfo.notRequired.keys
                    val customInfo = properties.filter { p -> totalPropertyList.find { it.equals(p.key, true) } == null }.let {
                        if (it.isNotEmpty()) it.accumulate((if (isCustomCustomResource) "\"${typeInfo.type}\", " else "") + "properties = mapOf(\n", ")", ",\n") {
                            "\"${it.key}\" to ${codeBuilder.valueString(it.value)}"
                        } else ""
                    }
                    codeBuilder.refBuilder = codeBuilder.refBuilder.copy(code = required + (if (notRequired.isEmpty()) "" else "{\n⇥$notRequired\n⇤}") + if (customInfo.isNotEmpty()) ".asCustomResource($customInfo)" else "")
                    codeBuilder
                }
        )

        private fun CodeBuilder.codeForMetadata(node: JsonNode) = node["Metadata"]?.let {
            codeFrom("metadata(${jsonFor(it)})\n")
        } ?: CodeBlock.of("")

        fun CodeBuilder.codeForOutputs(node: JsonNode) = codeFrom(node["Outputs"]?.let {
            it.fieldsAsMap().accumulate("outputs(\n⇥", "\n⇤)\n") { (name, fieldNode) ->
                this + name
                this + Output::class
                val fields = listOfNotNull(
                        fieldNode["Value"]?.let { "value = " + valueString(it) },
                        fieldNode["Description"]?.let { "description = \"${escape(it.textValue())}\"" },
                        fieldNode["Condition"]?.let { "condition = " + conditionReferenceFor(escape(it.textValue())) },
                        fieldNode["Export"]?.let {
                            it["Name"]?.let { exportName ->
                                this + Output.Export::class
                                "export = %T(" + valueString(exportName) + ")"
                            }
                        }
                ).accumulate()
                "%S to %T($fields)"
            }
        } ?: "")

        fun functionForTemplate(node: JsonNode): FunSpec {
            parameters.putAll(node.mapFromFieldNamed("Parameters"))
            conditions.putAll(node.mapFromFieldNamed("Conditions"))
            mappings.putAll(node.mapFromFieldNamed("Mappings"))
            resources.putAll(node.mapFromFieldNamed("Resources"))
            val outputCodeBuilder = CodeBuilder()
            val outputsCode = outputCodeBuilder.codeForOutputs(node)
            return FunSpec.builder(functionName)
                    .returns(KloudFormationTemplate::class)
                    .addCode("return %T.create {\n⇥⇥", KloudFormationTemplate::class)
                    .addCode(CodeBuilder().codeForParameters())
                    .addCode(CodeBuilder().codeForMetadata(node))
                    .addCode(CodeBuilder().codeForConditions())
                    .addCode(CodeBuilder().codeForMappings())
                    .addCode(codeForResources(outputCodeBuilder.refBuilder))
                    .addCode(outputsCode)
                    .addCode("⇤}\n⇤")
                    .build()
        }

        private fun classForTemplate(node: JsonNode) = TypeSpec.objectBuilder(className).addFunction(functionForTemplate(node)).build()

        data class RefBuilder(
            val name: String = "",
            val code: String = "",
            val refs: MutableList<String> = mutableListOf()
        )

        inner class CodeBuilder(val objectList: MutableList<Any> = mutableListOf(), var refBuilder: RefBuilder = RefBuilder()) {
            operator fun plus(item: Any) = objectList.add(item)
            fun codeFrom(code: String): CodeBlock {
                return CodeBlock.of(code, *objectList.toTypedArray())
            }
        }

        val depsResolved: (List<String>) -> (CodeBuilder) -> Boolean = { l ->
            { cb ->
                with(cb.refBuilder) {
                    when {
                        refs.isEmpty() -> true
                        else -> (l intersect refs) == refs.toSet()
                    }
                }
            }
        }

        private fun dependencyTreeFor(
            depTree: List<CodeBuilder>,
            resolvedDeps: List<CodeBuilder> = emptyList(),
            lastResolved: List<CodeBuilder> = emptyList()
        ): Pair<List<CodeBuilder>, List<CodeBuilder>> {
            val (resolved, unresolved) = depTree.partition { depsResolved(resolvedDeps.map { it.refBuilder.name })(it) }
            return if (resolved == lastResolved) resolvedDeps to unresolved
            else dependencyTreeFor(unresolved, resolvedDeps + resolved, resolved)
        }

        private fun reorder(codeBuilders: List<CodeBuilder>): CodeBlock {
            val codeBlocks = codeBuilders.toMutableList()
            val refCounts = codeBlocks.map { item -> item.refBuilder.name to codeBlocks.count { it.refBuilder.refs.contains(item.refBuilder.name) } }.toMap()
            val (orderedCodeBlocks, errorCodeDeps) = dependencyTreeFor(codeBlocks)
            return CodeBlock.builder().also { builder ->
                // appending error code deps to be sorted by the compiler for now
                (orderedCodeBlocks + errorCodeDeps).forEach { codeBlock ->
                    val code = if (refCounts[codeBlock.refBuilder.name] == 0) codeBlock.refBuilder.code
                    else "val ${codeBlock.refBuilder.name.variableName()} = ${codeBlock.refBuilder.code}"
                    builder.add(code + "\n", *codeBlock.objectList.toTypedArray())
                }
            }.build()
        }

        override fun deserialize(parser: JsonParser, context: DeserializationContext): FileSpec =
                FileSpec.builder("$kPackage.stack", className)
                        .addType(classForTemplate(parser.codec.readTree(parser)))
                        .also { file -> staticImports.forEach { (pkg, name) -> file.addImport(pkg, name) } }
                        .build()
    }
}