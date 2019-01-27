package io.kloudformation.specification

private typealias Properties<T> = Map<String, T>

object SpecificationMerger {
    private fun PropertyInfo.merge(b: PropertyInfo) = copy(properties = properties.merge(b.properties))
    private fun <T> Properties<T>.merge(b: Properties<T>, mergeValue: T.(T) -> T = { this }): Properties<T> {
        val (keysInBoth, keysOnlyInA) = keys.partition { b.containsKey(it) }
        val keysOnlyInB = b.keys.filter { !this.containsKey(it) }
        return (keysOnlyInA.map { it to this[it]!! } +
                keysOnlyInB.map { it to b[it]!! } +
                keysInBoth.map { it to this[it]!!.mergeValue(b[it]!!) }).toMap()
    }
    fun merge(specifications: List<Specification>) =
            specifications.reduce { mergedSpec, currentSpec ->
                mergedSpec.copy(
                        propertyTypes = mergedSpec.propertyTypes.merge(currentSpec.propertyTypes) { merge(it) },
                        resourceTypes = mergedSpec.resourceTypes.merge(currentSpec.resourceTypes) { merge(it) }
                )
            }
}