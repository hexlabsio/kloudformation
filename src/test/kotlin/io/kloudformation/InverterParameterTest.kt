package io.kloudformation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kloudformation.model.KloudFormationTemplate
import io.kloudformation.resource.ec2.RouteTable
import io.kloudformation.resource.ec2.routeTable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.reflect.full.functions
import kotlin.test.expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InverterParameterTest{

    val testClass = "Parameters"

    @Test
    fun `should have type String and correct logical name for empty object parameter`(){
        compare("""{ "TestParameter": {}}""", testClass, "simpleParameter") {
            with(Inverter.StackInverter(parameters = it.fieldsAsMap())) {
                CodeBuilder().codeForParameters().toString().trim()
            }
        }
    }

    @Test
    fun `should have type String but type set to Number when Number`(){
        compare("""{ "TestParameter": { "Type": "Number" }}""", testClass, "numberParameter") {
            with(Inverter.StackInverter(parameters = it.fieldsAsMap())) {
                CodeBuilder().codeForParameters().toString().trim()
            }
        }
    }
}