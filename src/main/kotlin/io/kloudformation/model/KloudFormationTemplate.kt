package io.kloudformation.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.cfg.MapperConfig
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.KloudResource
import io.kloudformation.Value
import io.kloudformation.function.Condition
import io.kloudformation.function.Intrinsic
import io.kloudformation.function.Reference

data class KloudFormationTemplate(
    val awsTemplateFormatVersion: String? = "2010-09-09",
    @JsonInclude(JsonInclude.Include.NON_NULL) val description: String? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val metadata: Value<JsonNode>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val parameters: Map<String, Parameter<*>>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val mappings: Map<String, Map<String, Map<String, Value<Any>>>>? = null,
    @JsonInclude(JsonInclude.Include.NON_NULL) val conditions: Map<String, Intrinsic>? = null,
    val resources: Resources,
    @JsonInclude(JsonInclude.Include.NON_NULL) val outputs: Map<String, Output>? = null
) {
    @JsonSerialize(using = Resources.Serializer::class)
    data class Resources(val resources: Map<String, KloudResource<*>>) {

        class Serializer : StdSerializer<Resources>(Resources::class.java) {
            override fun serialize(item: Resources, generator: JsonGenerator, provider: SerializerProvider) {
                val codec = generator.codec
                generator.writeStartObject()
                item.resources.forEach {
                    generator.writeObjectFieldStart(it.key)
                    generator.writeFieldName("Type")
                    generator.writeString(it.value.kloudResourceType)
                    if (it.value.dependsOn?.isEmpty() == false) {
                        generator.writeArrayFieldStart("DependsOn")
                        it.value.dependsOn!!.forEach { generator.writeString(it) }
                        generator.writeEndArray()
                    }
                    with(it.value.resourceProperties) {
                        if (metadata != null) {
                            generator.writeObjectField("Metadata", metadata)
                        }
                        if (!condition.isNullOrEmpty()) {
                            generator.writeFieldName("Condition")
                            generator.writeString(condition)
                        }
                        if (creationPolicy != null) generator.writeObjectField("CreationPolicy", creationPolicy)
                        if (updatePolicy != null) generator.writeObjectField("UpdatePolicy", updatePolicy)
                        if (deletionPolicy != null) generator.writeObjectField("DeletionPolicy", deletionPolicy)
                        if (codec is ObjectMapper) {
                            val props = codec.valueToTree<ObjectNode>(it.value)
                            otherProperties.orEmpty().forEach { name, value -> props.replace(name, codec.valueToTree<JsonNode>(value)) }
                            if (props.size() != 0) {
                                generator.writeFieldName("Properties")
                                generator.writeTree(props)
                            }
                        } else {
                            generator.writeObjectField("Properties", it.value)
                        }
                        generator.writeEndObject()
                    }
                }
                generator.writeEndObject()
            }
        }
    }

    class NamingStrategy : PropertyNamingStrategy() {
        override fun nameForGetterMethod(config: MapperConfig<*>?, method: AnnotatedMethod, defaultName: String?) =
                if (defaultName == "awsTemplateFormatVersion") "AWSTemplateFormatVersion"
                else method.name.substring(3)
    }

    class Builder(
        val awsTemplateFormatVersion: String? = "2010-09-09",
        val description: String? = null,
        private val resources: MutableList<KloudResource<String>> = mutableListOf(),
        private val parameters: MutableList<Parameter<*>> = mutableListOf(),
        private val mappings: MutableList<Pair<String, Map<String, Map<String, Value<Any>>>>> = mutableListOf(),
        private val conditions: MutableList<Pair<String, Intrinsic>> = mutableListOf(),
        private val outputs: MutableList<Pair<String, Output>> = mutableListOf(),
        private var metadata: Value<JsonNode>? = null,
        var currentDependees: List<String>? = null
    ) {
        fun <T : KloudResource<String>> add(resource: T): T = resource.also { this.resources.add(it) }
        fun build() = KloudFormationTemplate(
                awsTemplateFormatVersion = awsTemplateFormatVersion,
                description = description,
                resources = Resources(resources.map { it.logicalName to it }.toMap()),
                parameters = if (parameters.isEmpty()) null else parameters.map { it.logicalName to it }.toMap(),
                mappings = if (mappings.isEmpty()) null else mappings.toMap(),
                conditions = if (conditions.isEmpty())null else conditions.toMap(),
                outputs = if (outputs.isEmpty())null else outputs.toMap(),
                metadata = metadata
        )

        fun allocateLogicalName(logicalName: String): String {
            var index = 0
            fun nameFor(index: Int) = logicalName + if (index == 0)"" else "${index + 1}"
            index++
            while (resources.find { it.logicalName == nameFor(index - 1) } != null) {
                index++
            }
            return nameFor(index - 1)
        }

        fun <R, T : KloudResource<R>> T.then(builder: Builder.(T) -> Unit) = run { also {
            val previousDependees = currentDependees
            currentDependees = listOf(logicalName)
            this@Builder.builder(this)
            currentDependees = previousDependees
        } }

        fun <T : KloudResource<*>> Iterable<T>.then(builder: Builder.(Iterable<T>) -> Unit) = run { also {
            val previousDependees = currentDependees
            currentDependees = map { it.logicalName }
            this@Builder.builder(this)
            currentDependees = previousDependees
        } }

        operator fun <T> T.unaryPlus() = Value.Of(this)

        fun <T> parameter(
            logicalName: String,
            type: String = "String",
            allowedPattern: String? = null,
            allowedValues: List<String>? = null,
            constraintDescription: String? = null,
            default: String? = null,
            description: String? = null,
            maxLength: String? = null,
            maxValue: String? = null,
            minLength: String? = null,
            minValue: String? = null,
            noEcho: String? = null
        ) = Parameter<T>(logicalName, type, allowedPattern, allowedValues, constraintDescription, default, description, maxLength, maxValue, minLength, minValue, noEcho).also { parameters.add(it) }

        fun <T> parameter(
            logicalName: String,
            type: ParameterType<T>,
            allowedPattern: String? = null,
            allowedValues: List<String>? = null,
            constraintDescription: String? = null,
            default: String? = null,
            description: String? = null,
            maxLength: String? = null,
            maxValue: String? = null,
            minLength: String? = null,
            minValue: String? = null,
            noEcho: String? = null
        ): Parameter<T> = parameter(logicalName, type.type, allowedPattern, allowedValues, constraintDescription, default, description, maxLength, maxValue, minLength, minValue, noEcho)

        fun <T : Any> mappings(vararg mappings: Pair<String, Map<String, Map<String, Value<T>>>>) = also {
            this.mappings += mappings
        }
        fun conditions(vararg conditions: Pair<String, Intrinsic>) = also { this.conditions += conditions }
        fun condition(logicalName: String, predicate: Intrinsic) = let { this.conditions += logicalName to predicate; Condition(logicalName) }
        fun metadata(metadata: JsonNode) = metadata(Value.Of(metadata))
        fun metadata(metadata: Value<JsonNode>) = also { this.metadata = metadata }

        fun outputs(vararg outputs: Pair<String, Output>) = also { this.outputs += outputs }

        companion object {
            val awsAccountId = Reference<String>("AWS::AccountId")
            val awsNotificationArns = Reference<List<String>>("AWS::NotificationARNs")
            fun <T> awsNoValue() = Reference<T>("AWS::NoValue")
            val awsPartition = Reference<String>("AWS::Partition")
            val awsRegion = Reference<String>("AWS::Region")
            val awsStackId = Reference<String>("AWS::StackId")
            val awsStackName = Reference<String>("AWS::StackName")
            val awsUrlSuffix = Reference<String>("AWS::URLSuffix")
        }
    }

    companion object {
        fun create(awsTemplateFormatVersion: String? = "2010-09-09", description: String? = null, dsl: Builder.() -> Unit) = Builder(awsTemplateFormatVersion, description).apply(dsl).build()
    }
}
