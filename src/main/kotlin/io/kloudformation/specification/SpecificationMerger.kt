package io.kloudformation.specification

object SpecificationMerger {
    private fun PropertyInfo.merge(b: PropertyInfo) = copy(properties = properties.merge(b.properties))
    private fun <T> Map<String, T>?.merge(b: Map<String, T>?, mergeValue: T.(T) -> T = { this }): Map<String, T>? {
        return when {
            this == null -> b
            b == null -> this
            else -> {
                val (keysInBoth, keysOnlyInA) = keys.partition { b.containsKey(it) }
                val keysOnlyInB = b.keys.filter { !this.containsKey(it) }
                return (keysOnlyInA.map { it to this[it]!! } +
                        keysOnlyInB.map { it to b[it]!! } +
                        keysInBoth.map { it to this[it]!!.mergeValue(b[it]!!) }).toMap()
            }
        }
    }
    fun merge(specifications: List<Specification>) =
            specifications.reduce { mergedSpec, currentSpec ->
                mergedSpec.copy(
                        propertyTypes = mergedSpec.propertyTypes.merge(currentSpec.propertyTypes) { merge(it) }.orEmpty(),
                        resourceTypes = mergedSpec.resourceTypes.merge(currentSpec.resourceTypes) { merge(it) }.orEmpty()
                )
            }
}