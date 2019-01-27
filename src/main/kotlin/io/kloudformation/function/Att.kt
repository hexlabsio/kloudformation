package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer

@JsonSerialize(using = Att.Serializer::class)
data class Att<T>(val reference: String, val attribute: Att.Value<String>) :
        io.kloudformation.Value<T>, Select.ObjectValue<T>, SplitValue<T>, SubValue, IfValue<T> {

    interface Value<T>

    class Serializer : StdSerializer<Att<*>>(Att::class.java) {
        override fun serialize(item: Att<*>, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeArrayFieldStart("Fn::GetAtt")
            generator.writeString(item.reference)
            generator.writeObject(item.attribute)
            generator.writeEndArray()
            generator.writeEndObject()
        }
    }
}