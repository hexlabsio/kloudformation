package io.kloudformation.model.iam

import io.kloudformation.Value

data class Condition(val conditions: List<Conditional>)

sealed class Conditional {
    class Multi(val operator: String, val conditions: Map<String, List<Value<*>>>) : Conditional()
    class Solo(val operator: String, val conditions: Map<String, Value<*>>) : Conditional()
}

fun conditionals(operator: String, conditions: Map<String, List<Value<*>>>) = Conditional.Multi(operator, conditions)
fun conditional(operator: String, conditions: Map<String, Value<*>>) = Conditional.Solo(operator, conditions)
