package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.Value

interface SplitValue<T>
@JsonSerialize(using = Split.Serializer::class)
data class Split(val delimiter: String, val sourceString: SplitValue<String>) :
        Value<List<Value<String>>>, ImportValue.Value<List<String>>, Select.ObjectValue<List<Select.ObjectValue<String>>> {

    class Serializer : StdSerializer<Split>(Split::class.java) {
        override fun serialize(item: Split, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeArrayFieldStart("Fn::Split")
            generator.writeObject(item.delimiter)
            generator.writeObject(item.sourceString)
            generator.writeEndArray()
            generator.writeEndObject()
        }
    }
}