package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer

@JsonSerialize(using = Cidr.Serializer::class)
data class Cidr(val ipBlock: Cidr.Value<String>, val count: Cidr.Value<String>, val sizeMask: Cidr.Value<String>? = null) : io.kloudformation.Value<String> {

    interface Value<T>

    class Serializer : StdSerializer<Cidr>(Cidr::class.java) {
        override fun serialize(item: Cidr, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeArrayFieldStart("Fn::Cidr")
            generator.writeObject(item.ipBlock)
            generator.writeObject(item.count)
            if (item.sizeMask != null) generator.writeObject(item.sizeMask)
            generator.writeEndArray()
            generator.writeEndObject()
        }
    }
}
