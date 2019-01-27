package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.Value

@JsonSerialize(using = Select.Serializer::class)
data class Select<T>(val index: Select.IndexValue<String>, val objects: Select.ObjectValue<List<Select.ObjectValue<T>>>)
    : Value<T>, Cidr.Value<T>, ImportValue.Value<T>, SplitValue<T>, SubValue, IfValue<T> {

    interface IndexValue<T>
    interface ObjectValue<T>

    class Serializer : StdSerializer<Select<*>>(Select::class.java) {
        override fun serialize(item: Select<*>, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeArrayFieldStart("Fn::Select")
            generator.writeObject(item.index)
            val codec = generator.codec
            if (codec is ObjectMapper) {
                val props = codec.valueToTree<JsonNode>(item.objects)
                if (props is ObjectNode || props.size() != 1) generator.writeTree(props)
                else generator.writeTree(props[0])
            }

            generator.writeEndArray()
            generator.writeEndObject()
        }
    }
}
