package io.kloudformation

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import io.kloudformation.function.Reference
import io.kloudformation.function.plus

data class ResourceProperties(
    @JsonIgnore val condition: String? = null,
    @JsonIgnore val metadata: Value<JsonNode>? = null,
    @JsonIgnore val updatePolicy: UpdatePolicy? = null,
    @JsonIgnore val creationPolicy: CreationPolicy? = null,
    @JsonIgnore val deletionPolicy: String? = null,
    @JsonIgnore var otherProperties: Map<String, *>? = null
)

open class KloudResource<T>(
    @JsonIgnore open val logicalName: String,
    @JsonIgnore open val resourceProperties: ResourceProperties = ResourceProperties(),
    @JsonIgnore open var kloudResourceType: String = "AWS::CloudFormation::CustomResource",
    @JsonIgnore open val dependsOn: List<String>? = null
) {

    fun ref() = Reference<T>(logicalName)

    operator fun plus(other: String) = this.ref() + other

    operator fun <R> plus(other: Value<R>) = this.ref() + other
    operator fun <R> plus(other: KloudResource<R>) = this.ref() + other.ref()

    fun asCustomResource(resourceType: String = "AWS::CloudFormation::CustomResource", properties: Map<String, *> = emptyMap<String, String>()) = also {
        kloudResourceType = resourceType
        resourceProperties.otherProperties = properties
    }
}

interface KloudResourceBuilder

infix fun KloudResource<*>.and(b: KloudResource<*>) = listOf(this, b)

infix fun Iterable<KloudResource<*>>.and(b: KloudResource<*>) = this + b
