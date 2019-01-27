package io.kloudformation.function

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import io.kloudformation.Value

interface Intrinsic
interface ConditionalValue<T> : Intrinsic
@JsonSerialize(using = Conditional.Serializer::class)
interface Conditional : ConditionalValue<Boolean>, Value<Boolean> {
    class Serializer : StdSerializer<Conditional>(Conditional::class.java) {
        override fun serialize(item: Conditional, generator: JsonGenerator, provider: SerializerProvider) {
            generator.writeStartObject()
            when (item) {
                is And -> {
                    generator.writeArrayFieldStart("Fn::And")
                    generator.writeObject(item.a)
                    generator.writeObject(item.b)
                }
                is Equals -> {
                    generator.writeArrayFieldStart("Fn::Equals")
                    generator.writeObject(item.a)
                    generator.writeObject(item.b)
                }
                is Not -> {
                    generator.writeArrayFieldStart("Fn::Not")
                    generator.writeObject(item.a)
                }
                is Or -> {
                    generator.writeArrayFieldStart("Fn::Or")
                    generator.writeObject(item.a)
                    generator.writeObject(item.b)
                }
                is Condition -> {
                    generator.writeObjectField("Condition", item.logicalName)
                }
            }
            generator.writeEndArray()
            generator.writeEndObject()
        }
    }
}
data class Condition(val logicalName: String) : ConditionalValue<Boolean>, Conditional
data class And(val a: ConditionalValue<Boolean>, val b: ConditionalValue<Boolean>) : Conditional
data class Or(val a: ConditionalValue<Boolean>, val b: ConditionalValue<Boolean>) : Conditional

interface EqualsValue
data class Equals(val a: EqualsValue, val b: EqualsValue) : Conditional

data class Not(val a: ConditionalValue<Boolean>) : Conditional

infix fun ConditionalValue<Boolean>.and(b: ConditionalValue<Boolean>) = And(this, b)
infix fun ConditionalValue<Boolean>.or(b: ConditionalValue<Boolean>) = Or(this, b)
infix fun EqualsValue.eq(b: EqualsValue) = Equals(this, b)
fun not(value: ConditionalValue<Boolean>) = Not(value)
