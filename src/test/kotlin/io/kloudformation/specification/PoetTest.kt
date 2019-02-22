package io.kloudformation.specification

import com.squareup.kotlinpoet.TypeSpec
import io.kloudformation.Value
import org.junit.jupiter.api.Test
import kotlin.test.expect

class PoetTest {
    @Test
    fun `should generate file with a type and single primitive property`(){
        val spec = Specification(
                resourceSpecificationVersion = "2.23.0",
                propertyTypes = mapOf(
                        "AWS::EMR::InstanceGroupConfig.EbsBlockDeviceConfig" to PropertyInfo(
                                properties = mapOf(
                                        "VolumeSpecification" to Property(
                                                documentation = "",
                                                required = false,
                                                updateType = "Mutable",
                                                primitiveType = "String"
                                        )
                                )
                        )
                ),
                resourceTypes = emptyMap()
        )
        val files = SpecificationPoet.generateSpecs(spec)
        expect(1){ files.size }
        with(files.first()) {
            expect(2) { members.size }
            with(members[1] as TypeSpec){
                expect("EbsBlockDeviceConfig") { name }
                expect(1) { propertySpecs.size }
                with(propertySpecs.first()){
                    expect("volumeSpecification") { name }
                    expect("${Value::class ofType String::class}?") { type.toString() }
                }
            }
        }
    }

    @Test
    fun `should generate with correct proxy types`(){
        val spec = Specification(
                resourceSpecificationVersion = "2.23.0",
                propertyTypes = mapOf(
                        "AWS::EC2::LaunchTemplate.WebhookFilter" to PropertyInfo(
                                properties = mapOf(
                                        "A" to Property(
                                                documentation = "",
                                                required = true,
                                                updateType = "Mutable",
                                                primitiveType = "String"
                                        )
                                )
                        ),
                        "AWS::EC2::LaunchTemplate.FilterGroup" to PropertyInfo(
                                type = "List",
                                required = false,
                                itemType = "WebhookFilter",
                                updateType = "Mutable"
                        ),
                        "AWS::EC2::LaunchTemplate.CapacityReservationPreference" to PropertyInfo(
                                primitiveType = "String"
                        ),
                        "AWS::EC2::LaunchTemplate.Listy" to PropertyInfo(
                                type = "List",
                                primitiveItemType = "String"
                        ),
                        "AWS::EC2::LaunchTemplate.LaunchTemplateData" to PropertyInfo(
                                properties = mapOf(
                                        "FilterGroups" to Property(
                                                documentation = "",
                                                required = true,
                                                updateType = "Mutable",
                                                type = "List",
                                                itemType = "FilterGroup"
                                        ),
                                        "CapacityReservationPreference" to Property(
                                                documentation = "",
                                                required = true,
                                                updateType = "Mutable",
                                                type = "CapacityReservationPreference"
                                        ),
                                        "OtherList" to Property(
                                                documentation = "",
                                                required = true,
                                                updateType = "Mutable",
                                                type = "List",
                                                itemType = "Listy"
                                        )
                                )
                        )
                ),
                resourceTypes = emptyMap()
        )
        val files = SpecificationPoet.generateSpecs(spec)
        expect(2){ files.size }
        with(files.first()) {
            expect(2) { members.size }
            with(members[1] as TypeSpec){
                expect("WebhookFilter") { name }
            }
        }
    }
}