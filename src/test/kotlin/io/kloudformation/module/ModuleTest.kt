package io.kloudformation.module

import io.kloudformation.KloudFormation
import io.kloudformation.Value
import io.kloudformation.function.plus
import io.kloudformation.model.KloudFormationTemplate
import io.kloudformation.resource.aws.s3.Bucket
import io.kloudformation.resource.aws.s3.bucket
import org.junit.jupiter.api.Test
import kotlin.test.expect

class TestModule(val bucket: Bucket, val child: TestModule?) : Module {
    class BucketProps(var bucketName: Value<String>) : Properties
    class Predefined(var parentName: Value<String>) : Properties
    class Parts {
        val testBucket = modification<Bucket.Builder, Bucket, BucketProps>()
        val child = submodule { pre: Predefined -> Builder(pre) }
    }
    class Builder(pre: Predefined) : SubModuleBuilder<TestModule, Parts, Predefined>(pre, Parts()) {
        override fun KloudFormation.buildModule(): Parts.() -> TestModule = {
            val bucketResource = testBucket(BucketProps(pre.parentName)) { props ->
                bucket {
                    bucketName(props.bucketName)
                    modifyBuilder(props)
                }
            }
            val childModule = child.module(Predefined(bucketResource.bucketName!! + "-child"))()
            TestModule(bucketResource, childModule)
        }
    }
}
fun KloudFormation.testModule(bucketName: String, partBuilder: TestModule.Parts.() -> Unit = {}) = builder(TestModule.Builder(TestModule.Predefined(+bucketName)), partBuilder)

class ModuleTest {
    @Test
    fun `should create parent bucket`() {
        val template = KloudFormationTemplate.create {
            testModule("parent")
        }
        with(template.resources.resources.toList()) {
            expect(1) { size }
            with(first().second) {
                expect("AWS::S3::Bucket") { this.kloudResourceType }
                with(this as Bucket) {
                    expect("parent") { bucketName!!.value() }
                }
            }
        }
    }
    @Test
    fun `should create parent bucket with updated name from modify`() {
        val template = KloudFormationTemplate.create {
            testModule("parent") { testBucket { bucketName("other") } }
        }
        with(template.resources.resources.toList()) {
            expect(1) { size }
            with(first().second) {
                expect("AWS::S3::Bucket") { this.kloudResourceType }
                with(this as Bucket) {
                    expect("other") { bucketName!!.value() }
                }
            }
        }
    }
    @Test
    fun `should create parent bucket with updated name from props`() {
        val template = KloudFormationTemplate.create {
            testModule("parent") { testBucket.props { bucketName = +"other" } }
        }
        with(template.resources.resources.toList()) {
            expect(1) { size }
            with(first().second) {
                expect("AWS::S3::Bucket") { this.kloudResourceType }
                with(this as Bucket) {
                    expect("other") { bucketName!!.value() }
                }
            }
        }
    }
    @Test
    fun `should create child bucket with name from parent`() {
        val template = KloudFormationTemplate.create {
            testModule("parent") {
                child()
            }
        }
        with(template.resources.resources.toList()) {
            expect(2) { size }
            with(last().second) {
                expect("AWS::S3::Bucket") { this.kloudResourceType }
                with(this as Bucket) {
                    expect(Value.Of("parent") + "-child") { bucketName }
                }
            }
        }
    }
    @Test
    fun `should create child bucket with name from modification`() {
        val template = KloudFormationTemplate.create {
            testModule("parent") {
                child { testBucket { bucketName("other") } }
            }
        }
        with(template.resources.resources.toList()) {
            expect(2) { size }
            with(last().second) {
                expect("AWS::S3::Bucket") { this.kloudResourceType }
                with(this as Bucket) {
                    expect("other") { bucketName!!.value() }
                }
            }
        }
    }
    @Test
    fun `should create child bucket with name from updated property`() {
        val template = KloudFormationTemplate.create {
            testModule("parent") {
                child { testBucket.props { bucketName = +"otherParent" } }
            }
        }
        with(template.resources.resources.toList()) {
            expect(2) { size }
            with(last().second) {
                expect("AWS::S3::Bucket") { this.kloudResourceType }
                with(this as Bucket) {
                    expect("otherParent") { bucketName!!.value() }
                }
            }
        }
    }
    @Test
    fun `should create child bucket with name from modification using updated property`() {
        val template = KloudFormationTemplate.create {
            testModule("parent") {
                child {
                    testBucket.props { bucketName = +"otherParent" }
                    testBucket { props -> bucketName(props.bucketName + "-xxx") }
                }
            }
        }
        with(template.resources.resources.toList()) {
            expect(2) { size }
            with(last().second) {
                expect("AWS::S3::Bucket") { this.kloudResourceType }
                with(this as Bucket) {
                    expect(Value.Of("otherParent") + "-xxx") { bucketName }
                }
            }
        }
    }
    @Test
    fun `should create child buckets for nested children`() {
        val template = KloudFormationTemplate.create {
            testModule("parent") {
                child { child() }
            }
        }
        with(template.resources.resources.toList()) {
            expect(3) { size }
            with(first().second) {
                expect("AWS::S3::Bucket") { this.kloudResourceType }
                with(this as Bucket) {
                    expect(Value.Of("parent")) { bucketName }
                }
            }
            with(get(1).second) {
                expect("AWS::S3::Bucket") { this.kloudResourceType }
                with(this as Bucket) {
                    expect(Value.Of("parent") + "-child") { bucketName }
                }
            }
            with(get(2).second) {
                expect("AWS::S3::Bucket") { this.kloudResourceType }
                with(this as Bucket) {
                    expect(Value.Of("parent") + "-child" + "-child") { bucketName }
                }
            }
        }
    }
}