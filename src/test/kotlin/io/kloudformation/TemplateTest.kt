package io.kloudformation

import com.squareup.kotlinpoet.FunSpec
import io.kloudformation.model.KloudFormationTemplate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TemplateTest{

    private val testClass = "CrossTemplate"

    private fun compare(template: String, function: String) = compare(template, testClass, function) {
        val fields = it.fieldsAsMap()
        val parameters = fields["Parameters"]!!.fieldsAsMap()
        val resources = fields["Resources"]!!.fieldsAsMap()

        with(Inverter.StackInverter(parameters = parameters, resources = resources)) {
            val outputCodeBuilder = CodeBuilder()
            val outputsCode = outputCodeBuilder.codeForOutputs(it)
                    CodeBuilder().codeForParameters().toString().trim() + "\n" +
                    codeForResources(outputCodeBuilder.refBuilder).toString().trim() + "\n" +
                            outputsCode.toString().trim()
        }
    }

    @Test
    fun `should set reference variables of parameters resources and outputs`() = compare(templateFrom("crossTemplateReferences.json"), "crossTemplateReferences")

}