package io.kloudformation

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InverterParameterTest {

    private val testClass = "Parameters"

    private fun compare(template: String, function: String) = compare(template, testClass, function) {
        with(Inverter.StackInverter(parameters = it.fieldsAsMap(), classPackage = "", fileName = "")) {
            CodeBuilder().codeForParameters().toString().trim()
        }
    }

    @Test
    fun `should have type String and correct logical name for empty object parameter`() = compare("""{ "TestParameter": {}}""", "simpleParameter")

    @Test
    fun `should have type String but type set to Number when Number`() = compare("""{ "TestParameter": { "Type": "Number" }}""", "numberParameter")

    @Test
    fun `should have type String but type set to ListNumber when ListNumber`() = compare("""{ "TestParameter": { "Type": "List<Number>" }}""", "numberListParameter")

    @Test
    fun `should have type String but type set to CommaDelimitedList when CommaDelimitedList`() = compare("""{ "TestParameter": { "Type": "CommaDelimitedList" }}""", "commaParameter")

    @Test
    fun `should have escaped allowedPattern`() = compare("""{ "TestParameter": { "AllowedPattern": "\\d+" }}""", "escapedParameter")
}