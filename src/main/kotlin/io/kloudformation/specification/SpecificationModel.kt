package io.kloudformation.specification

data class Specification(
    val propertyTypes: Map<String, PropertyInfo>,
    val resourceTypes: Map<String, PropertyInfo>,
    val resourceSpecificationVersion: String
)

data class PropertyInfo(
    val properties: Map<String, Property>,
    val documentation: String? = null,
    val attributes: Map<String, Attribute>? = null,
    val additionalProperties: Boolean? = null
)

data class Property(
    val documentation: String,
    val required: Boolean,
    val updateType: String,
    val type: String? = null,
    val duplicatesAllowed: Boolean? = null,
    val itemType: String? = null,
    val primitiveType: String? = null,
    val primitiveItemType: String? = null
)

data class Attribute(
    val type: String? = null,
    val dataSourceArn: String? = null,
    val primitiveType: String? = null,
    val primitiveItemType: String? = null
)