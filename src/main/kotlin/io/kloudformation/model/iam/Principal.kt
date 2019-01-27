package io.kloudformation.model.iam

import io.kloudformation.Value

open class Principal(open val principals: Pair<PrincipalType, List<Value<String>>>)
data class NotPrincipal(override val principals: Pair<PrincipalType, List<Value<String>>>) : Principal(principals)
val allPrincipals = Principal(PrincipalType.ALL to emptyList())
val noPrincipals = NotPrincipal(PrincipalType.ALL to emptyList())
enum class PrincipalType(val principal: String) {
    ALL("*"),
    AWS("AWS"),
    FEDERATED("Federated"),
    SERVICE("Service");
}