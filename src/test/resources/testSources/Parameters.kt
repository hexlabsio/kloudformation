package testSources

import io.kloudformation.model.KloudFormationTemplate

object Parameters{
    fun KloudFormationTemplate.Builder.simpleParameter(){
        val testParameter = parameter<kotlin.String>(logicalName = "TestParameter")
    }
    fun KloudFormationTemplate.Builder.numberParameter(){
        val testParameter = parameter<kotlin.String>(logicalName = "TestParameter", type = "Number")
    }
    fun KloudFormationTemplate.Builder.numberListParameter(){
        val testParameter = parameter<kotlin.collections.List<kotlin.String>>(logicalName = "TestParameter", type = "List<Number>")
    }
    fun KloudFormationTemplate.Builder.commaParameter(){
        val testParameter = parameter<kotlin.collections.List<kotlin.String>>(logicalName = "TestParameter", type = "CommaDelimitedList")
    }
    fun KloudFormationTemplate.Builder.escapedParameter(){
        val testParameter = parameter<kotlin.String>(logicalName = "TestParameter", allowedPattern = "\\d+")
    }
}