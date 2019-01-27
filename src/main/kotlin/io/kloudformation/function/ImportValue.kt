package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer

@JsonSerialize(using = ImportValue.Serializer::class)
data class ImportValue<T>(val sharedValueToImport: ImportValue.Value<String>) :
        io.kloudformation.Value<T>, SubValue {

    interface Value<T>

    class Serializer : StdSerializer<ImportValue<*>>(ImportValue::class.java) {
        override fun serialize(item: ImportValue<*>, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeObjectField("Fn::ImportValue", item.sharedValueToImport)
            generator.writeEndObject()
        }
    }
}