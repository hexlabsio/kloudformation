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
class InverterResourceTest{

    private val jackson = jacksonObjectMapper()
    private fun String.fieldsAsMap() = jackson.readValue<JsonNode>(this).fields().asSequence().toList().map { it.key to it.value }.toMap().toMutableMap()

    private fun match(a: String, b: String){
        fun String.strip() = lines().filter { it.isNotEmpty() }.map { it.trim() }
        expect(b.strip()){ a.strip() }
    }

    @Test
    fun `should call function for correct type with correct logical name`(){
        val resources = """{
            "WindowsServerWaitHandle" : {
                    "Type" : "AWS::CloudFormation::WaitConditionHandle"
            }
        }""".fieldsAsMap()
        with(Inverter.StackInverter(resources = resources)) {
            expect("""waitConditionHandle(logicalName = "WindowsServerWaitHandle")"""){
                codeForResources().toString().trim()
            }
        }
    }

    @Test
    fun `should store variable and reference it when referenced by logical name`(){
        val resources = """{
            "MyTopic":{ "Type" : "AWS::SNS::Topic" },
            "MySubscription": {
              "Type" : "AWS::SNS::Subscription",
              "Properties" : {
                "TopicArn" : { "Ref": "MyTopic" }
              }
            }
        }""".fieldsAsMap()
        with(Inverter.StackInverter(resources = resources)) {
            match("""val myTopic = topic(logicalName = "MyTopic")
                    subscription(logicalName = "MySubscription"){
                        topicArn(myTopic.ref())
                    }""",
                codeForResources().toString()
            )
        }
    }
}