package io.kloudformation.specification

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.readValue
import io.kloudformation.specification.SpecificationMerger.merge
import java.net.URL
import java.util.zip.GZIPInputStream

private val jacksonObjectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
private val specificationListUrl = "https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cfn-resource-specification.html"

fun main() {
    SpecificationPoet.generate(SpecificationScraper
        .scrapeLinks(specificationListUrl)
        .mapNotNull { (specificationName, specificationUrl) ->
            try {
                URL(specificationUrl).openStream().use { stream ->
                    System.out.println("Downloading $specificationName from $specificationUrl")
                    specificationName to if (specificationUrl.contains("gzip")) {
                        GZIPInputStream(stream).bufferedReader().readText()
                    } else {
                        stream.bufferedReader().readText()
                    }
                }.asSpecification()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        .merge()
    )
}

private fun Pair<String, String>.asSpecification() =
    try {
        println("Reading $first")
        jacksonObjectMapper.readValue<Specification>(second)
    } catch (ex: Exception) {
        System.err.println("Failed to parse $first specification")
        ex.printStackTrace()
        Specification(emptyMap(), emptyMap(), "")
    }
