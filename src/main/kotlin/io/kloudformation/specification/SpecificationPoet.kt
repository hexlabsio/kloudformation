package io.kloudformation.specification

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import io.kloudformation.KloudResource
import io.kloudformation.KloudResourceBuilder
import io.kloudformation.ResourceProperties
import io.kloudformation.Value
import io.kloudformation.function.Att
import io.kloudformation.model.KloudFormationTemplate
import java.io.File
import java.lang.RuntimeException
import kotlin.reflect.KClass

object SpecificationPoet {

    private const val logicalName = "logicalName"
    private const val dependsOn = "dependsOn"
    private const val resourceProperties = "resourceProperties"

    private val builderFunctionResourceParameters = listOf(
            ParameterSpec.builder(logicalName, String::class.asClassName().copy(true)).defaultValue("null").build(),
            ParameterSpec.builder(dependsOn, (List::class ofType String::class).copy(true)).defaultValue("null").build(),
            ParameterSpec.builder(resourceProperties, ResourceProperties::class).defaultValue("%T()", ResourceProperties::class).build()
    )
    private fun TypeSpec.Builder.addResourceConstructorParameters() = also {
        addSuperclassConstructorParameter("$logicalName·=·$logicalName")
        addSuperclassConstructorParameter("$dependsOn·=·$dependsOn")
        addSuperclassConstructorParameter("$resourceProperties·=·$resourceProperties")

        addProperty(PropertySpec.builder(logicalName, String::class, KModifier.OVERRIDE).initializer(logicalName).build())
        addProperty(PropertySpec.builder(dependsOn, (List::class ofType String::class).copy(true), KModifier.OVERRIDE).initializer(dependsOn).addAnnotation(JsonIgnore::class).build())
        addProperty(PropertySpec.builder(resourceProperties, ResourceProperties::class, KModifier.OVERRIDE).initializer(resourceProperties).addAnnotation(JsonIgnore::class).build())
    }

    private fun FunSpec.Builder.addResourceConstructorParameters() = also {
        addParameter(ParameterSpec.builder(dependsOn, (List::class ofType String::class).copy(true)).defaultValue("null").build())
        addParameter(ParameterSpec.builder(resourceProperties, ResourceProperties::class).defaultValue("%T()", ResourceProperties::class).build())
    }

    private fun TypeSpec.Builder.addBuilderResourceProperties() = also {
        addProperty(PropertySpec.builder(dependsOn, (List::class ofType String::class).copy(true)).initializer(dependsOn).build())
        addProperty(PropertySpec.builder(resourceProperties, ResourceProperties::class).initializer(resourceProperties).build())
    }

    private fun FunSpec.Builder.addResourceParameters() = also {
        addParameter(ParameterSpec.builder(dependsOn, (List::class ofType String::class).copy(true)).defaultValue("null").build())
        addParameter(ParameterSpec.builder(resourceProperties, ResourceProperties::class).defaultValue("%T()", ResourceProperties::class).build())
    }

    private data class TypeInfo(val awsTypeName: String, val canonicalName: String, val properties: List<PropertyTypeInfo>)
    private data class PropertyTypeInfo(val name: String, val typeName: TypeName)

    private data class TypeMappings(
        val properties: Map<String, PropertyInfo>,
        val resources: Map<String, PropertyInfo>,
        val types: Map<String, PropertyInfo>,
        val proxyTypes: Map<String, TypeName>
    ) {
        companion object {
            fun from(specification: Specification): TypeMappings {
                val proxyTypes = specification.propertyTypes.filter { it.value.properties == null }.toMap()
                val propertyTypes = specification.propertyTypes.filter { it.value.properties != null }.toMap()
                val types = (propertyTypes + specification.resourceTypes)
                val proxyMappings = proxyTypes.map { (key, info) -> key to getType(types.keys, emptyMap(), key, info.primitiveType, info.primitiveItemType, info.itemType, info.type) }.toMap()
                return TypeMappings(propertyTypes, specification.resourceTypes, types, proxyMappings)
            }
        }
    }

    fun generateSpecs(specification: Specification): List<FileSpec> {
        val typeMappings = TypeMappings.from(specification)
        val files = (typeMappings.properties
                .map { it.key to buildFile(typeMappings.types.keys, typeMappings.proxyTypes, false, it.key, it.value) } +
                specification.resourceTypes
                        .map { it.key to buildFile(typeMappings.types.keys, typeMappings.proxyTypes, true, it.key, it.value) }).toMap()

        val fieldMappings = files.map {
            TypeInfo(
                    awsTypeName = it.key,
                    canonicalName = it.value.packageName + "." + (it.value.members.first { it is TypeSpec } as TypeSpec).name,
                    properties = (it.value.members.first { it is TypeSpec } as TypeSpec).propertySpecs.map {
                        PropertyTypeInfo(it.name, it.type)
                    }
            )
        }
//        println("| CloudFormation | KloudFormation |")
//        println("|---|---|")
//        fieldMappings.filter { it.canonicalName.startsWith("io.kloudformation.resource") }.map {
//                it.awsTypeName.split("::").take(2).joinToString(" ") to (it.awsTypeName to it.canonicalName)
//        }.sortedBy { it.first }.groupBy { it.first }.forEach { (group, resources) ->
//            println("|**$group**||")
//            resources.sortedBy { it.second.first }.forEach { (_, pair) -> println("| ${pair.first} | ${pair.second} |") }
//        }
        return files.map { file ->
            val type = file.value.members.first { it is TypeSpec } as TypeSpec
            val propertyType = file.key
            val propertyInfo = typeMappings.types[propertyType]
            val isResource = specification.resourceTypes.containsKey(propertyType)
            FileSpec.builder(file.value.packageName, file.value.name)
                    .also { newFile ->
                        file.value.members.filter { it is FunSpec }.map { it as FunSpec }.forEach { newFile.addFunction(it) }
                    }
                    .addType(
                            type.toBuilder()
                                    .primaryConstructor(type.primaryConstructor)
                                    .addType(companionObject(typeMappings.types.keys, typeMappings.proxyTypes, isResource, propertyType, propertyInfo!!))
                                    .addType(builderClass(typeMappings.types.keys, typeMappings.proxyTypes, isResource, propertyType, propertyInfo, fieldMappings))
                                    .build()
                    )
                    .build()
        }
    }

    fun generate(specification: Specification) {
        File(System.getProperty("user.dir") + "/src/main/resources/info.json").writeText(libraryInfoFrom(specification))
        generateSpecs(specification).forEach { it.writeTo(File(System.getProperty("user.dir") + "/build/generated")) }
    }

    data class Info(val type: String, val name: String, val resource: Boolean, val required: Map<String, String>, val notRequired: Map<String, String>)

    private fun infoFrom(types: Set<String>, proxies: Map<String, TypeName>, resource: Boolean, typeName: String, propertyInfo: PropertyInfo): Info {
        val properties = propertyInfo.properties.orEmpty().run { filter { it.value.required } to filter { !it.value.required } }
        return Info(typeName, getClassName(typeName), resource,
                properties.first.map { it.key.decapitalize() to awsNameFor(getType(types, proxies, typeName, it.value).toString()) }.toMap(),
                properties.second.map { it.key.decapitalize() to awsNameFor(getType(types, proxies, typeName, it.value).toString()) }.toMap()
        )
    }

    private fun awsNameFor(type: String): String = if (type.startsWith("io.kloudformation.property.aws")) {
        val parts = type.substringAfter("io.kloudformation.property.aws.")
        val firstPart = parts.substringBefore(".")
        if (firstPart == "Tag") "Tag" else {
            val rest = parts.substringAfter("$firstPart.")
                    .split(".").foldIndexed(firstPart.capitalize() + "::") { index, acc, it ->
                        "$acc${if (index > 0) "." else ""}${it.capitalize()}"
                    }
            "AWS::$rest"
        }
    } else if (type.startsWith("kotlin.collections.List<io.kloudformation.property."))
        "List<${awsNameFor(type.substringAfter("kotlin.collections.List<").substringBeforeLast(">"))}>"
    else type

    private fun libraryInfoFrom(specification: Specification): String {
        val typeMappings = TypeMappings.from(specification)
        return jacksonObjectMapper().writeValueAsString((typeMappings.properties.map {
            it.key to infoFrom(typeMappings.types.keys, typeMappings.proxyTypes, false, it.key, it.value)
        } + typeMappings.resources.map { it.key to infoFrom(typeMappings.types.keys, typeMappings.proxyTypes, true, it.key, it.value) }).toMap())
    }

    private fun buildFile(types: Set<String>, proxies: Map<String, TypeName>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo) =
            FileSpec.builder(getPackageName(isResource, typeName), getClassName(typeName))
                    .addType(buildType(types, proxies, isResource, typeName, propertyInfo))
                    .addFunction(builderFunction(types, proxies, isResource, typeName, propertyInfo))
                    .build()

    private fun builderClassNameFrom(type: String) = ClassName.bestGuess("$type.Builder")

    private fun builderFunction(types: Set<String>, proxies: Map<String, TypeName>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo) = FunSpec.let {
        val name = getClassName(typeName)
        val packageName = getPackageName(isResource, typeName)
        it.builder(name.decapitalize()).also { func ->
            if (isResource) {
                func.addCode("return add(builder(%T.create(${paramListFrom(propertyInfo, true, true, name)})).build())\n", ClassName.bestGuess("$packageName.$name"))
            } else {
                func.addCode("return builder(%T.create(${paramListFrom(propertyInfo, false)})).build()\n", ClassName.bestGuess("$packageName.$name"))
            }
            propertyInfo.properties.orEmpty().sorted().filter { it.value.required }.map { func.addParameter(buildParameter(types, proxies, typeName, it.key, it.value)) }
            if (isResource) func.addParameters(builderFunctionResourceParameters)
            val builderTypeName = builderClassNameFrom("$packageName.$name")
            func.addParameter(ParameterSpec.builder("builder", LambdaTypeName.get(builderTypeName, returnType = builderTypeName)).defaultValue("{·this·}").build())
        }
                .apply { if (isResource) receiver(KloudFormationTemplate.Builder::class) }
                .build()
    }

    private fun buildType(types: Set<String>, proxies: Map<String, TypeName>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo) =
            TypeSpec.classBuilder(getClassName(typeName))
                    .addModifiers(if (!propertyInfo.properties.orEmpty().isEmpty() || isResource) KModifier.DATA else KModifier.PUBLIC)
                    .primaryConstructor(if (!propertyInfo.properties.orEmpty().isEmpty() || isResource) buildConstructor(types, proxies, isResource, typeName, propertyInfo) else null)
                    .also {
                        if (isResource)
                            it
                                    .superclass(KloudResource::class ofType String::class)
                                    .addSuperclassConstructorParameter("kloudResourceType·=·%S", typeName)
                                    .addResourceConstructorParameters()
                    }
                    .addFunctions(functionsFrom(types, proxies, typeName, propertyInfo.attributes.orEmpty()))
                    .addProperties(propertyInfo.properties.orEmpty().sorted().map { buildProperty(types, proxies, typeName, it.key, it.value) })
                    .build()

    private fun buildConstructor(types: Set<String>, proxies: Map<String, TypeName>, isResource: Boolean, classTypeName: String, propertyInfo: PropertyInfo) =
            FunSpec.constructorBuilder()
                    .also { if (isResource) it.addParameter(ParameterSpec.builder(logicalName, String::class, KModifier.OVERRIDE).addAnnotation(JsonIgnore::class).build()) }
                    .addParameters(propertyInfo.properties.orEmpty().toList().sortedWith(compareBy({ !it.second.required }, { it.first })).toMap().map { buildParameter(types, proxies, classTypeName, it.key, it.value) })
                    .also { if (isResource) it.addResourceConstructorParameters() }
                    .build()

    private fun functionsFrom(types: Set<String>, proxies: Map<String, TypeName>, typeName: String, attributes: Map<String, Attribute>) = attributes.map {
        FunSpec.builder(escape(it.key)).addCode("return %T<%T>(logicalName, %T(%S))\n", Att::class, getType(types, proxies, typeName, it.value, false), Value.Of::class, it.key).build()
    }

    private fun Map<String, Property>.sorted() = toList().sortedWith(compareBy({ !it.second.required }, { it.first })).toMap()

    private fun builderConstructor(types: Set<String>, proxies: Map<String, TypeName>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo) = FunSpec
            .constructorBuilder()
            .also { func ->
                if (isResource) {
                    func.addParameter(ParameterSpec.builder(logicalName, String::class).build())
                }
                func.addParameters(propertyInfo.properties.orEmpty().sorted().filter { it.value.required }.map { buildParameter(types, proxies, typeName, it.key, it.value) })
                if (isResource) func.addResourceParameters()
            }
            .build()

    private fun companionObject(types: Set<String>, proxies: Map<String, TypeName>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo) = TypeSpec.companionObjectBuilder()
            .addFunction(buildCreateFunction(types, proxies, isResource, typeName, propertyInfo))
            .build()

    private fun builderClass(types: Set<String>, proxies: Map<String, TypeName>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo, typeMappings: List<TypeInfo>) = TypeSpec.classBuilder("Builder")
            .primaryConstructor(builderConstructor(types, proxies, isResource, typeName, propertyInfo))
            .addSuperinterface(KloudResourceBuilder::class)
            .also { if (isResource) it.addProperty(PropertySpec.builder(logicalName, String::class).initializer(logicalName).build()) }
            .addProperties(propertyInfo.properties.orEmpty().sorted().let {
                it.filter { !it.value.required }.map { buildVarProperty(types, proxies, typeName, it.key, it.value) } +
                        it.filter { it.value.required }.map { buildProperty(types, proxies, typeName, it.key, it.value) }
            })
            .also { if (isResource) it.addBuilderResourceProperties() }
            .addFunctions(
                    propertyInfo.properties.orEmpty().filter { !it.value.required }.flatMap {
                        listOfNotNull(
                                if (it.value.itemType == null && (it.value.primitiveType != null || it.value.primitiveItemType != null))
                                    primitiveSetterFunction(it.key.decapitalize(), it.value, getType(types, proxies, typeName, it.value, wrapped = false))
                                else null,
                                if (it.value.primitiveType == null && it.value.primitiveItemType == null && it.value.itemType == null && it.value.type != null) {
                                    val proxy = findProxy(proxies, typeName, it.value.type!!)
                                    if (proxy == null) typeSetterFunction(it.key, it.key, typeName, typeMappings)
                                    else null
                                } else null,
                                FunSpec.builder(it.key.decapitalize())
                                        .addParameter(it.key.decapitalize(), getType(types, proxies, typeName, it.value))
                                        .addCode("return also·{ it.${it.key.decapitalize()}·=·${it.key.decapitalize()} }\n")
                                        .build()
                        )
                    } + listOf(
                            FunSpec.builder("build")
                                    .also {
                                        val primitiveProperties = propertyInfo.properties.orEmpty().keys + (if (isResource) setOf(resourceProperties, "dependsOn") else emptySet())
                                        it.addCode("return ${getClassName(typeName)}( " + primitiveProperties.foldIndexed(if (isResource) logicalName + (if (primitiveProperties.isNotEmpty()) ", " else "") else "") {
                                            index, acc, item -> acc + (if (index != 0)", " else "") + "${item.decapitalize()}·=·${item.decapitalize()}"
                                        } + ")\n")
                                    }
                                    .build()
                    )
            )
            .build()

    private fun paramListFrom(propertyInfo: PropertyInfo, isResource: Boolean, addCurrentDependee: Boolean = false, specialLogicalName: String? = null): String {
        val nameList = (if (isResource) listOf(logicalName, dependsOn) else emptyList()) +
                propertyInfo.properties.orEmpty().sorted().filter { it.value.required }.keys.map { it.decapitalize() } +
                (if (isResource) listOf(resourceProperties) else emptyList())
        return nameList.foldIndexed("") {
            index, acc, name -> acc + (if (index != 0) ", " else "") + "$name·=·" +
                (
                        if (name == dependsOn && addCurrentDependee) "$name·?:·currentDependees"
                        else if (name == logicalName && !specialLogicalName.isNullOrEmpty()) "$name·?:·allocateLogicalName(\"$specialLogicalName\")"
                        else name
                        )
        }
    }

    private fun buildCreateFunction(types: Set<String>, proxies: Map<String, TypeName>, isResource: Boolean, typeName: String, propertyInfo: PropertyInfo): FunSpec {
        val funSpec = if (isResource) {
            FunSpec.builder("create").addParameter(logicalName, String::class).addCode("return Builder(${paramListFrom(propertyInfo, true)})\n")
        } else FunSpec.builder("create").addCode("return Builder(${paramListFrom(propertyInfo, false)})\n")
        propertyInfo.properties.orEmpty().sorted().filter { it.value.required }.forEach { funSpec.addParameter(buildParameter(types, proxies, typeName, it.key, it.value)) }
        if (isResource) funSpec.addResourceParameters()
        return funSpec.build()
    }

    private fun primitiveSetterFunction(name: String, property: Property, type: TypeName) = FunSpec.builder(name + if (property.type == "Map") "Map" else "")
            .addParameter(name, type)
            .also {
                if (property.primitiveItemType != null) {
                    if (property.type == "Map") it.addCode("return also·{ it.$name·= $name.orEmpty().map·{ it.key to %T(it.value) }.toMap() }\n", Value.Of::class)
                    else it.addCode("return also·{ it.$name·= %T($name) }\n", Value.Of::class)
                } else if (property.primitiveType != null) {
                    it.addCode("return also·{ it.$name·= %T($name) }\n", Value.Of::class)
                }
            }
            .build()

    private fun childParams(parameters: Collection<String>) = parameters.foldIndexed("") { index, acc, parameter -> acc + (if (index != 0) ", " else "") + parameter }

    private fun typeSetterFunction(name: String, propertyType: String, typeName: String, typeMappings: List<TypeInfo>): FunSpec {
        val parent = (typeMappings.find { it.awsTypeName == typeName }!!.properties.find { it.name == propertyType.decapitalize() && it.typeName.toString().startsWith("io") }!!.typeName as ClassName)
        val requiredProperties = typeMappings.find { it.canonicalName == parent.canonicalName }!!.properties.filter { !it.typeName.isNullable }
        val propertyNames = requiredProperties.map { it.name }

        return FunSpec.builder(name.decapitalize())
                .addParameters(requiredProperties.map { ParameterSpec.builder(it.name, it.typeName).build() })
                .addParameter(
                        ParameterSpec.builder(
                                name = "builder",
                                type = LambdaTypeName.get(
                                        receiver = ClassName.bestGuess("${parent.canonicalName}.Builder"),
                                        returnType = ClassName.bestGuess("${parent.canonicalName}.Builder")
                                )
                        ).defaultValue("{·this·}").build()
                )
                .addCode("return ${name.decapitalize()}(%T.create(${childParams(propertyNames)}).builder().build())\n", ClassName.bestGuess(parent.canonicalName))
                .build()
    }

    private fun buildProperty(types: Set<String>, proxies: Map<String, TypeName>, classTypeName: String, propertyName: String, property: Property) =
            PropertySpec.builder(
                    propertyName.decapitalize(),
                    if (property.required) getType(types, proxies, classTypeName, property).copy(false) else getType(types, proxies, classTypeName, property).copy(true))
                    .initializer(propertyName.decapitalize())
                    .build()

    private fun buildVarProperty(types: Set<String>, proxies: Map<String, TypeName>, classTypeName: String, propertyName: String, property: Property) =
            PropertySpec.builder(
                    propertyName.decapitalize(),
                    getType(types, proxies, classTypeName, property).copy(true)
            ).mutable(true).initializer("null").build()

    private fun buildParameter(types: Set<String>, proxies: Map<String, TypeName>, classTypeName: String, parameterName: String, property: Property) =
            if (property.required) ParameterSpec
                    .builder(parameterName.decapitalize(), getType(types, proxies, classTypeName, property).copy(false))
                    .build()
            else ParameterSpec
                    .builder(parameterName.decapitalize(), getType(types, proxies, classTypeName, property).copy(true))
                    .defaultValue("null")
                    .build()

    private fun getClassName(typeName: String) =
            typeName.split("::", ".").last()

    private fun getPackageName(isResource: Boolean, typeName: String): String {
        val subPackage = "io.kloudformation.${if (isResource) "resource" else "property"}"
        return subPackage + if (typeName.contains("::")) typeName.split("::", ".").dropLast(1).joinToString(".", ".").toLowerCase() else ""
    }

    private fun primitiveTypeName(primitiveType: String) = ClassName.bestGuess(primitiveType.replace("Json", "com.fasterxml.jackson.databind.JsonNode").replace("Map", "com.fasterxml.jackson.databind.JsonNode").replace("Timestamp", "java.time.Instant").replace("Integer", "kotlin.Int").replace("String", "kotlin.String"))

    private fun valueTypeName(primitiveType: String, wrapped: Boolean) =
            if (wrapped) Value::class ofType primitiveTypeName(primitiveType)
            else primitiveTypeName(primitiveType)

    private fun getType(types: Set<String>, proxies: Map<String, TypeName>, classTypeName: String, attribute: Attribute, wrapped: Boolean = true) =
            getType(types, proxies, classTypeName, attribute.primitiveType, attribute.primitiveItemType, null, attribute.type, wrapped)

    private fun getType(types: Set<String>, proxies: Map<String, TypeName>, classTypeName: String, property: Property, wrapped: Boolean = true) =
            getType(types, proxies, classTypeName, property.primitiveType, property.primitiveItemType, property.itemType, property.type, wrapped)

    private fun getType(types: Set<String>, proxies: Map<String, TypeName>, classTypeName: String, primitiveType: String?, primitiveItemType: String?, itemType: String? = null, type: String? = null, wrapped: Boolean = true) = when {
        !primitiveType.isNullOrEmpty() -> {
            if (wrapped) Value::class ofType primitiveTypeName(primitiveType)
            else primitiveTypeName(primitiveType)
        }
        !primitiveItemType.isNullOrEmpty() -> {
            if (type.equals("Map")) Map::class.ofTypes(String::class.asTypeName(), valueTypeName(primitiveItemType, wrapped))
            else {
                val arrayOfValueOfType = List::class ofType valueTypeName(primitiveItemType, true)
                if (wrapped) Value::class ofType arrayOfValueOfType else arrayOfValueOfType
            }
        }
        !itemType.isNullOrEmpty() -> {
            List::class ofType (findProxy(proxies, classTypeName, itemType) ?: ClassName.bestGuess(getPackageName(false, getTypeName(types, classTypeName, itemType.toString())) + "." + itemType))
        }
        type == null -> String::class.asTypeName()
        else -> findProxy(proxies, classTypeName, type.toString()) ?: ClassName.bestGuess(getPackageName(false, getTypeName(types, classTypeName, type.toString())) + "." + type)
    }

    private fun getTypeName(types: Set<String>, classTypeName: String, propertyType: String) =
            types.filter { it == propertyType || it.endsWith(".$propertyType") }.let {
                when {
                    it.size > 1 -> it.first { it.contains(classTypeName.split("::").last().split(".").first()) }
                    it.size == 1 -> it.first()
                    else -> {
                        println("Could not find type name for $classTypeName $propertyType")
                        "kotlin.String"
                    }
                }
            }

    private fun findProxy(proxies: Map<String, TypeName>, classTypeName: String, propertyType: String) =
            proxies[classTypeName.substringBeforeLast("::") + "::" + classTypeName.substringAfterLast("::").substringBefore(".") + ".$propertyType"]

    private fun escape(name: String) = name.replace(".", "")
}

fun KClass<*>.ofTypes(vararg types: TypeName) = this.asClassName().parameterizedBy(*types.toList().toTypedArray())
infix fun KClass<*>.ofType(type: TypeName) = this.asClassName().parameterizedBy(type)
infix fun KClass<*>.ofType(type: KClass<*>) = this.asClassName() ofType(type.asTypeName())
infix fun ClassName.ofType(type: TypeName) = this.parameterizedBy(type)