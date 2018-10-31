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

    private val jackson = jacksonObjectMapper()
    private fun String.fieldsAsMap() = jackson.readValue<JsonNode>(this).fields().asSequence().toList().map { it.key to it.value }.toMap().toMutableMap()

    @Test
    fun `should have type String and correct logical name for empty object parameter`(){
        val parameters = """{ "TestParameter": {}}""".fieldsAsMap()
        val testClass = InverterParameterTest::class.java.classLoader.getResource("testSources/test.kt").readText().lines()
        data class Acc(var inFunction: Boolean = false, val lines: MutableList<String> = mutableListOf(), var opens: Int = 0)
        val lines = testClass.fold(Acc()){
            acc, line ->
            if(line.trim() == "}"){
                acc.opens--
                if(acc.opens == 0){
                    acc.inFunction = false
                }
            }
            if(acc.inFunction){
                acc.lines += line
            }
            if(line.trim() == "fun KloudFormationTemplate.Builder.TestOne(){"){
                acc.opens++
                acc.inFunction = true
            }
                acc
        }.lines
        println(lines)
        with(Inverter.StackInverter(parameters = parameters)) {
            expect("""val testParameter = parameter<kotlin.String>(logicalName = "TestParameter")"""){
                CodeBuilder().codeForParameters().toString().trim()
            }
        }
    }

    @Test
    fun `should have type String but type set to Number when Number`(){
        val parameters = """{ "TestParameter": { "Type": "Number" }}""".fieldsAsMap()
        with(Inverter.StackInverter(parameters = parameters)) {
            expect("""val testParameter = parameter<kotlin.String>(logicalName = "TestParameter", type = "Number")"""){
                CodeBuilder().codeForParameters().toString().trim()
            }
        }
    }
}