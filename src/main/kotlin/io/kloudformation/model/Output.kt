package io.kloudformation.model

import io.kloudformation.Value

data class Output(val value: Value<*>, val description: String? = null, val condition: String? = null, val export: Export? = null) {
    data class Export(val name: Value<String>)
}