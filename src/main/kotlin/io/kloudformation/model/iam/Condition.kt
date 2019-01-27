package io.kloudformation.model.iam

import io.kloudformation.Value

data class Condition(val conditions: List<Conditional<*, *>>)

class Conditional<S, out T : ConditionOperator<S>>(val operator: T, val conditions: Map<ConditionKey<S>, List<Value<String>>>)

fun conditional(operator: String, conditions: Map<String, List<Value<String>>>) = Conditional(ConditionOperator(operator), conditions.map { ConditionKey<Any>(it.key) to it.value }.toMap())

open class ConditionOperator<T>(val operation: String)
object ConditionOperators {
    val stringEquals = ConditionOperator<String>("StringEquals")
    val stringNotEquals = ConditionOperator<String>("StringNotEquals")
    val numericEquals = ConditionOperator<Int>("NumericEquals")
}

open class ConditionKey<T>(val key: String)
object ConditionKeys {
    val awsUserName = ConditionKey<String>("aws:username")
    val s3MaxKeys = ConditionKey<Int>("s3:max-keys")
}
