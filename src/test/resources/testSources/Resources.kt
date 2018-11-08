package testSources

import io.kloudformation.model.KloudFormationTemplate
import io.kloudformation.resource.cloudformation.waitConditionHandle

object Resources{
    fun KloudFormationTemplate.Builder.nameCheck(){
        waitConditionHandle(logicalName = "WindowsServerWaitHandle")
    }
}