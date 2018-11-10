package testSources

import io.kloudformation.model.KloudFormationTemplate
import io.kloudformation.resource.cloudformation.waitCondition
import io.kloudformation.resource.cloudformation.waitConditionHandle
import io.kloudformation.resource.sns.subscription
import io.kloudformation.resource.sns.topic
import io.kloudformation.resource.sqs.queue

object CrossTemplate {
    fun KloudFormationTemplate.Builder.crossTemplateReferences(){
        val queueName = parameter<kotlin.String>(logicalName = "QueueName", description = "Name of an existing EC2 KeyPair")
        val windowsServerWaitHandle = waitConditionHandle(logicalName = "WindowsServerWaitHandle")
        val myQueue = queue(logicalName = "MyQueue"){
            queueName(queueName.ref())
        }
        val myTopic = topic(logicalName = "MyTopic")
        waitCondition(logicalName = "WindowsServerWaitCondition", dependsOn = listOf("WindowsServer"), handle = windowsServerWaitHandle.ref(), timeout = +"1800")
        val subscription = subscription(logicalName = "Subscription"){
            topicArn(myTopic.ref())
        }
        outputs(
                "SubscriptionArn" to io.kloudformation.model.Output(value = subscription.ref(), description = "ARN of Subscription"), "QueueARN" to io.kloudformation.model.Output(value = io.kloudformation.function.Att<kotlin.String>(myQueue.logicalName, +"Arn"), description = "ARN of newly created SQS Queue")
        )
    }
}