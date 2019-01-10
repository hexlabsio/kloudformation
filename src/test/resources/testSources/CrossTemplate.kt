package testSources

import io.kloudformation.model.KloudFormationTemplate
import io.kloudformation.resource.aws.sqs.queue
import io.kloudformation.resource.aws.cloudformation.waitCondition
import io.kloudformation.resource.aws.cloudformation.waitConditionHandle
import io.kloudformation.resource.aws.sns.subscription
import io.kloudformation.resource.aws.sns.topic

object CrossTemplate {
    fun KloudFormationTemplate.Builder.crossTemplateReferences(){
        val queueName = parameter<kotlin.String>(logicalName = "QueueName", description = "Name of an existing EC2 KeyPair")
        val windowsServerWaitHandle = waitConditionHandle(logicalName = "WindowsServerWaitHandle")
        val myQueue = queue(logicalName = "MyQueue"){
            queueName(queueName.ref())
        }
        waitCondition(logicalName = "WindowsServerWaitCondition", dependsOn = listOf("WindowsServer")){
            handle(windowsServerWaitHandle.ref())
            timeout(+"1800")
        }
        val subscription = subscription(logicalName = "Subscription"){
            topicArn(myTopic.ref())
        }
        val myTopic = topic(logicalName = "MyTopic")
        outputs(
                "SubscriptionArn" to io.kloudformation.model.Output(value = subscription.ref(), description = "ARN of Subscription"), "QueueARN" to io.kloudformation.model.Output(value = io.kloudformation.function.Att<kotlin.String>(myQueue.logicalName, +"Arn"), description = "ARN of newly created SQS Queue")
        )
    }
}