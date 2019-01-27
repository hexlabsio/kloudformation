package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.Value

@JsonSerialize(using = GetAZs.Serializer::class)
data class GetAZs(val region: GetAZs.Value<String>) :
        Value<List<Value<String>>>, Select.ObjectValue<List<Select.ObjectValue<String>>>, SplitValue<List<String>>, SubValue, IfValue<List<String>> {

    interface Value<T>

    class Serializer : StdSerializer<GetAZs>(GetAZs::class.java) {
        override fun serialize(item: GetAZs, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            generator.writeObjectField("Fn::GetAZs", item.region)
            generator.writeEndObject()
        }
    }
}