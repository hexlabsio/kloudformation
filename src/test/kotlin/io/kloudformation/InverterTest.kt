package io.kloudformation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kloudformation.model.KloudFormationTemplate
import io.kloudformation.resource.ec2.RouteTable
import io.kloudformation.resource.ec2.routeTable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.expect

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InverterTest{

    private val jackson = jacksonObjectMapper()
    private fun String.fieldsAsMap() = jackson.readValue<JsonNode>(this).fields().asSequence().toList().map { it.key to it.value }.toMap().toMutableMap()
    @Test
    fun go(){
        val parameters = """{ "TestParamter": {}}""".fieldsAsMap()
        with(Inverter.StackInverter(parameters = parameters)) {
            expect("""val testParamter = parameter<kotlin.String>(logicalName = "TestParamter")"""){
                CodeBuilder().codeForParameters().toString().trim()
            }
        }
    }
}