package io.kloudformation.model.iam

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.Value

@JsonSerialize(using = PolicyStatement.Serializer::class)
data class PolicyStatement(
    val action: Action,
    val effect: IamPolicyEffect = IamPolicyEffect.Allow,
    val resource: Resource? = null,
    val sid: String? = null,
    val principal: Principal? = null,
    val condition: Condition? = null
) {

    class Serializer : StdSerializer<PolicyStatement>(PolicyStatement::class.java) {
        override fun serialize(item: PolicyStatement, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            if (!item.sid.isNullOrBlank()) {
                generator.writeFieldName("Sid")
                generator.writeString(item.sid)
            }
            generator.writeFieldName("Effect")
            generator.writeString(item.effect.effect)
            generator.writeArrayFieldStart(if (item.action is NotAction) "NotAction" else "Action")
            item.action.actions.forEach { generator.writeObject(it) }
            generator.writeEndArray()
            if (item.resource != null && item.resource.resources.isNotEmpty()) {
                generator.writeArrayFieldStart(if (item.resource is NotResource) "NotResource" else "Resource")
                    item.resource.resources.forEach { generator.writeObject(it) }
                generator.writeEndArray()
            }
            if (item.principal != null) {
                val itemName = if (item.principal is NotPrincipal) "NotPrincipal" else "Principal"
                when (item.principal.principals.first) {
                    PrincipalType.ALL -> generator.writeStringField(itemName, "*")
                    else -> {
                        generator.writeObjectFieldStart(itemName)
                        generator.writeArrayFieldStart(item.principal.principals.first.principal)
                        item.principal.principals.second.forEach { generator.writeObject(it) }
                        generator.writeEndArray()
                        generator.writeEndObject()
                    }
                }
            }
            if (item.condition != null && item.condition.conditions.isNotEmpty()) {
                generator.writeObjectFieldStart("Condition")
                item.condition.conditions.forEach {
                    generator.writeObjectFieldStart(it.operator.operation)
                    it.conditions.forEach { (key, conditions) ->
                        generator.writeArrayFieldStart(key.key)
                        conditions.forEach { condition -> generator.writeObject(condition) }
                        generator.writeEndArray()
                    }
                    generator.writeEndObject()
                }
                generator.writeEndObject()
            }
            generator.writeEndObject()
        }
    }

    data class Builder(val effect: IamPolicyEffect = IamPolicyEffect.Allow, val action: Action, val resource: Resource? = null, val sid: String? = null) {
        var principal: Pair<PrincipalType, List<Value<String>>>? = null
        val conditionals: MutableList<Conditional<*, *>> = mutableListOf()
        var notPrincipal: Boolean = false

        fun principal(principalType: PrincipalType, principal: List<Value<String>>, notPrincipal: Boolean = false) = also {
            this.principal = principalType to principal
            this.notPrincipal = notPrincipal
        }
        fun notPrincipal(principalType: PrincipalType, principal: List<Value<String>>) = principal(principalType, principal, true)

        fun allPrincipals() = principal(PrincipalType.ALL, emptyList())
        fun noPrincipals() = notPrincipal(PrincipalType.ALL, emptyList())

        fun <S, T : ConditionOperator<S>> condition(operator: T, conditions: Map<ConditionKey<S>, List<Value<String>>>) = also { conditionals.add(Conditional(operator, conditions)) }
        fun condition(operator: String, conditions: Map<String, List<Value<String>>>) = also { conditionals.add(conditional(operator, conditions)) }

        fun build() = PolicyStatement(
                action = action,
                resource = resource,
                effect = effect,
                sid = sid,
                principal = principal?.let { (if (notPrincipal)NotPrincipal(principal!!) else Principal(principal!!)) },
                condition = if (conditionals.isEmpty()) null else Condition(conditionals)
        )
    }

    companion object {
        fun create(action: Action, resource: Resource? = null, sid: String? = null, effect: IamPolicyEffect = IamPolicyEffect.Allow) =
                Builder(action = action, resource = resource, sid = sid, effect = effect)
    }
}