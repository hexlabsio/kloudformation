package io.kloudformation.model.iam

import io.kloudformation.Value

open class Resource(open val resources: List<Value<String>>)
data class NotResource(override val resources: List<Value<String>>) : Resource(resources)
val allResources = Resource(listOf(Value.Of("*")))
val noResources = NotResource(listOf(Value.Of("*")))

fun resource(resource: String) = resources(resource)
fun resources(vararg resources: String) = Resource(resources.toList().map { Value.Of(it) })
fun resource(resource: Value<String>) = resources(resource)
fun resources(vararg resources: Value<String>) = Resource(resources.toList())
fun notResource(resource: String) = notResources(resource)
fun notResources(vararg resources: String) = NotResource(resources.toList().map { Value.Of(it) })
fun notResource(resource: Value<String>) = notResources(resource)
fun notResources(vararg resources: Value<String>) = NotResource(resources.toList())