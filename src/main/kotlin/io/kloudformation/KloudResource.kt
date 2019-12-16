package io.kloudformation

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import io.kloudformation.function.Reference
import io.kloudformation.function.plus

data class ResourceProperties(
    @JsonIgnore var condition: String? = null,
    @JsonIgnore var metadata: Value<JsonNode>? = null,
    @JsonIgnore var updatePolicy: UpdatePolicy? = null,
    @JsonIgnore var creationPolicy: CreationPolicy? = null,
    @JsonIgnore var deletionPolicy: String? = null,
    @JsonIgnore var otherProperties: Map<String, *>? = null
)

open class KloudResource<T>(
    @JsonIgnore open var logicalName: String,
    @JsonIgnore open var resourceProperties: ResourceProperties = ResourceProperties(),
    @JsonIgnore open var kloudResourceType: String = "AWS::CloudFormation::CustomResource",
    @JsonIgnore open var dependsOn: List<String>? = null
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
