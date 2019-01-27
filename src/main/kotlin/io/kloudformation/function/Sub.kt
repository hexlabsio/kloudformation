package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.Value

interface SubValue
@JsonSerialize(using = Sub.Serializer::class)
data class Sub(
    val string: String,
    val variables: Map<String, SubValue> = emptyMap()
) : Value<String>, ImportValue.Value<String>, IfValue<String> {

    class Serializer : StdSerializer<Sub>(Sub::class.java) {
        override fun serialize(item: Sub, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            if (item.variables.isEmpty()) {
                generator.writeObjectField("Fn::Sub", item.string)
            } else {
                generator.writeArrayFieldStart("Fn::Sub")
                generator.writeObject(item.string)
                generator.writeObject(item.variables)
                generator.writeEndArray()
            }
            generator.writeEndObject()
        }
    }
}