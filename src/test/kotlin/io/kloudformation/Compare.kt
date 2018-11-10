package io.kloudformation

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.lang.AssertionError
import kotlin.test.expect

private val jackson = jacksonObjectMapper()
fun templateFrom(fileName: String) = Match::class.java.classLoader.getResource(fileName).readText()
fun JsonNode.fieldsAsMap() = fields().asSequence().map { it.key to it.value }.toMap().toMutableMap()
interface MatchNoMatch
data class Match(val line: String): MatchNoMatch
data class NoMatch(val actual: String, val expected: String): MatchNoMatch
fun compare(template: String, testClassName: String, function: String, inversion: (JsonNode) -> String){
    val testClass = InverterParameterTest::class.java.classLoader.getResource("testSources/$testClassName.kt").readText().lines()
    data class Acc(var inFunction: Boolean = false, val lines: MutableList<String> = mutableListOf(), var opens: Int = 0)
    val lines = testClass.fold(Acc()){
        acc, line ->
        if(line.trim() == "}" && acc.inFunction){
            acc.opens--
            if(acc.opens == 0){
                acc.inFunction = false
            }
        }
        if(acc.inFunction){
            acc.lines += line
        }
        if(line.trim() == "fun KloudFormationTemplate.Builder.$function(){"){
            acc.opens++
            acc.inFunction = true
        }
        acc
    }.lines.map { it.trim() }.filter { it.isNotEmpty() }
    val actual = inversion(jackson.readValue(template)).lines().map { it.trim() }.filter { it.isNotEmpty() }
    if(lines.isEmpty()){
        throw AssertionError("Function with name $function was not found in test file named $testClassName\n${actual.fold(""){acc, it -> "$acc\n$it"}}")
    }
    val matchList = lines.foldIndexed(listOf<MatchNoMatch>()){
        index, acc, line ->
        val actualLine = if(actual.size > index) actual[index] else ""
        try{
            expect(line){ actualLine }
            acc + Match(actualLine)
        } catch(e: AssertionError){
            acc + NoMatch(actualLine, line)
        }
    }
    if(matchList.find { it is NoMatch } != null){
        throw AssertionError(
               matchList.foldIndexed("\n") { index, acc, match ->
                   val line = "line " + String.format("%1$5s: ", index)
                   when(match){
                   is Match -> acc + line + match.line + "\n"
                   is NoMatch -> acc + line + "\n**EXPECTED:\n    [" + match.expected +"]\n**ACTUAL:\n    [" + match.actual +"]\n\n"
                    else -> acc
               }}
        )
    }
}