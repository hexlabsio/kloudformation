package io.kloudformation.model.iam

import com.fasterxml.jackson.databind.JsonNode
import io.kloudformation.Value
import io.kloudformation.model.KloudFormationDsl

data class PolicyDocument(
    val statement: List<PolicyStatement>,
    val id: String? = null,
    val version: String? = null
) : Value<JsonNode> {

    @KloudFormationDsl
    data class Builder(val id: String? = null, val version: String? = null, val statement: MutableList<PolicyStatement> = mutableListOf()) {
        fun statement(action: Action, effect: Effect = Effect.Allow, resource: Resource? = null, sid: String? = null, builder: PolicyStatement.Builder.() -> Unit = { }) = also {
            statement.add(PolicyStatement.create(effect = effect, action = action, resource = resource, sid = sid).let { it.builder(); it.build() })
        }

        fun build() = PolicyDocument(statement, id, version)
    }

    companion object {
        fun create(id: String? = null, version: String? = null) = Builder(id, version)
    }
}

fun policyDocument(id: String? = null, version: String? = Version.V2.version, builder: PolicyDocument.Builder.() -> Unit)
        = PolicyDocument.create(id, version).let { it.builder(); it.build() }