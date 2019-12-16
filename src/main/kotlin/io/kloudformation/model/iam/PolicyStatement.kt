package io.kloudformation.model.iam

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.Value
import io.kloudformation.model.KloudFormationDsl

@JsonSerialize(using = PolicyStatement.Serializer::class)
data class PolicyStatement(
        val action: Action,
        val effect: Effect = Effect.Allow,
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
                    when (it) {
                        is Conditional.Multi -> {
                            generator.writeObjectFieldStart(it.operator)
                            it.conditions.forEach { (key, conditions) ->
                                generator.writeArrayFieldStart(key)
                                conditions.forEach { condition -> generator.writeObject(condition) }
                                generator.writeEndArray()
                            }
                            generator.writeEndObject()
                        }
                        is Conditional.Solo -> {
                            generator.writeObjectFieldStart(it.operator)
                            it.conditions.forEach { (key, condition) -> generator.writeObjectField(key, condition) }
                            generator.writeEndObject()
                        }
                    }
                }
                generator.writeEndObject()
            }
            generator.writeEndObject()
        }
    }
    @KloudFormationDsl
    data class Builder(val effect: Effect = Effect.Allow, val action: Action, val resource: Resource? = null, val sid: String? = null) {
        var principal: Pair<PrincipalType, List<Value<String>>>? = null
        val conditionals: MutableList<Conditional> = mutableListOf()
        var notPrincipal: Boolean = false

        fun principal(principalType: PrincipalType, principal: List<Value<String>>, notPrincipal: Boolean = false) = also {
            this.principal = principalType to principal
            this.notPrincipal = notPrincipal
        }
        fun notPrincipal(principalType: PrincipalType, principal: List<Value<String>>) = principal(principalType, principal, true)

        fun allPrincipals() = principal(PrincipalType.ALL, emptyList())
        fun noPrincipals() = notPrincipal(PrincipalType.ALL, emptyList())

        fun condition(operator: String, condition: Map<String, Value<*>>) = also { conditionals.add(conditional(operator, condition)) }
        fun conditions(operator: String, conditions: Map<String, List<Value<*>>>) = also { conditionals.add(conditionals(operator, conditions)) }

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
        fun create(action: Action, resource: Resource? = null, sid: String? = null, effect: Effect = Effect.Allow) =
                Builder(action = action, resource = resource, sid = sid, effect = effect)
    }
}