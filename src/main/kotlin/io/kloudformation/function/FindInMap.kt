package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.Value

interface FindInMapValue<T>

@JsonSerialize(using = FindInMap.Serializer::class)
data class FindInMap<T>(
    val mapName: FindInMapValue<String>,
    val topLevelKey: FindInMapValue<String>,
    val secondLevelKey: FindInMapValue<String>
) : Value<T>,
        FindInMapValue<T>,
        ImportValue.Value<T>,
        SplitValue<T>,
        Select.IndexValue<T>,
        Select.ObjectValue<T>,
        SubValue,
        IfValue<T>,
        ConditionalValue<T> {
    class Serializer : StdSerializer<FindInMap<*>>(FindInMap::class.java) {
        override fun serialize(item: FindInMap<*>, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeArrayFieldStart("Fn::FindInMap")
            generator.writeObject(item.mapName)
            generator.writeObject(item.topLevelKey)
            generator.writeObject(item.secondLevelKey)
            generator.writeEndArray()
            generator.writeEndObject()
        }
    }
}