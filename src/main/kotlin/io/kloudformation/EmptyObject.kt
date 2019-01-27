package io.kloudformation

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer

@JsonSerialize(using = EmptyObject.Serializer::class)
class EmptyObject<T> : Value<T> {
    class Serializer : StdSerializer<EmptyObject<*>>(EmptyObject::class.java) {
        override fun serialize(value: EmptyObject<*>, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeEndObject()
        }
    }
}