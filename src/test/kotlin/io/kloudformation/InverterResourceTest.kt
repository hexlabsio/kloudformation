package io.kloudformation

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InverterResourceTest{

    private val testClass = "Resources"

    private fun compare(template: String, function: String) = compare(template, testClass, function) {
        with(Inverter.StackInverter(resources = it.fieldsAsMap())) {
            codeForResources().toString().trim()
        }
    }

    @Test
    fun `should call function for correct type with correct logical name`() = compare("""{
            "WindowsServerWaitHandle" : {
                    "Type" : "AWS::CloudFormation::WaitConditionHandle"
            }
        }""", "nameCheck")
}