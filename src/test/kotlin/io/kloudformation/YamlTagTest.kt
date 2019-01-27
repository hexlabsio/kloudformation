package io.kloudformation

import org.junit.jupiter.api.Test
import kotlin.test.expect

class YamlTagTest {
    @Test
    fun `should convert bang Ref to Ref`() {
        val yaml = """ABC: !Ref Test"""
        expect("""ABC: {Ref: Test}""".trimMargin()) { mapToStandard(yaml) }
    }
    @Test
    fun `should convert bang GetAtt array to FnGetAtt`() {
        val yaml = """ABC: !GetAtt [Test, Other]"""
        expect("""ABC:
                 |  Fn::GetAtt: [Test, Other]""".trimMargin()) { mapToStandard(yaml) }
    }
    @Test
    fun `should convert bang GetAtt string to FnGetAtt`() {
        val yaml = """ABC: !GetAtt Test.Other"""
        expect("""ABC:
                 |  Fn::GetAtt: [Test, Other]""".trimMargin()) { mapToStandard(yaml) }
    }
}