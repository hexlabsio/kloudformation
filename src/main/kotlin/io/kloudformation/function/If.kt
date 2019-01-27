package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer

interface IfValue<T>
@JsonSerialize(using = If.Serializer::class)
data class If<T>(val condition: String, val trueValue: IfValue<T>, val falseValue: IfValue<T>) :
        io.kloudformation.Value<T>, ImportValue.Value<T>, Select.ObjectValue<T>, SplitValue<T>, SubValue, IfValue<T>, Intrinsic {

    class Serializer : StdSerializer<If<*>>(If::class.java) {
        override fun serialize(item: If<*>, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeArrayFieldStart("Fn::If")
            generator.writeObject(item.condition)
            generator.writeObject(item.trueValue)
            generator.writeObject(item.falseValue)
            generator.writeEndArray()
            generator.writeEndObject()
        }
    }
}