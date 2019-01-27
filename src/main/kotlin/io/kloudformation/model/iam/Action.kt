package io.kloudformation.model.iam

open class Action(open val actions: List<String>)
data class NotAction(override val actions: List<String>) : Action(actions)
val allActions = Action(listOf("*"))
val noActions = NotAction(listOf("*"))
fun action(action: String) = actions(action)
fun actions(vararg actions: String) = Action(actions.toList())
fun notAction(action: String) = notActions(action)
fun notActions(vararg actions: String) = NotAction(actions.toList())