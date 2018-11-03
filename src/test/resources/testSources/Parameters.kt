package testSources

import io.kloudformation.model.KloudFormationTemplate

object Parameters{
    fun KloudFormationTemplate.Builder.simpleParameter(){
        val testParameter = parameter<kotlin.String>(logicalName = "TestParameter")
    }
    fun KloudFormationTemplate.Builder.numberParameter(){
        val testParameter = parameter<kotlin.String>(logicalName = "TestParameter", type = "Number")
    }
}