package testSources

import io.kloudformation.json
import io.kloudformation.model.KloudFormationTemplate
import io.kloudformation.resource.aws.cloudformation.waitConditionHandle
import io.kloudformation.resource.aws.sqs.queue

object Resources{
    fun KloudFormationTemplate.Builder.nameCheck(){
        waitConditionHandle(logicalName = "WindowsServerWaitHandle")
    }
    fun KloudFormationTemplate.Builder.redrivePolicy(){
        queue(logicalName = "MySourceQueue"){
            redrivePolicy(json(
                    mapOf(
                            "deadLetterTargetArn" to mapOf(
                                    "Fn::GetAtt" to listOf(
                                            "MyDeadLetterQueue", "Arn"
                                    )
                            ), "maxReceiveCount" to 5
                    )
            ))
        }
    }
}