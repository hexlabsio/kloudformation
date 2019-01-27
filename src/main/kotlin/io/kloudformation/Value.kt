package io.kloudformation

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.function.Att
import io.kloudformation.function.Cidr
import io.kloudformation.function.ConditionalValue
import io.kloudformation.function.EqualsValue
import io.kloudformation.function.FindInMapValue
import io.kloudformation.function.GetAZs
import io.kloudformation.function.IfValue
import io.kloudformation.function.ImportValue
import io.kloudformation.function.Select
import io.kloudformation.function.SplitValue
import io.kloudformation.function.SubValue
import io.kloudformation.metadata.CfnCommand
import io.kloudformation.metadata.CfnInit

interface Value<out T> {
    @JsonSerialize(using = Of.Serializer::class)
    data class Of<T>(val value: T)
        : Value<T>,
            Cidr.Value<T>,
            Att.Value<T>,
            Select.IndexValue<T>,
            Select.ObjectValue<T>,
            GetAZs.Value<T>,
            ImportValue.Value<T>,
            SplitValue<T>,
            SubValue,
            IfValue<T>,
            EqualsValue,
            ConditionalValue<T>,
            FindInMapValue<T>,
            CfnCommand.Value<T>,
            CfnInit.Value<T> {
        class Serializer : StdSerializer<Value.Of<*>>(Value.Of::class.java) {
            override fun serialize(item: Value.Of<*>, generator: JsonGenerator, provider: SerializerProvider) {
                generator.writeObject(item.value)
            }
        }
    }
}

@JsonSerialize(using = JsonValue.Serializer::class)
data class JsonValue(val json: Map<String, Any>) : Value<JsonNode> {
    class Serializer : StdSerializer<JsonValue>(JsonValue::class.java) {
        override fun serialize(item: JsonValue, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeObject(item.json)
        }
    }
}

fun json(json: Map<String, Any>) = JsonValue(json)