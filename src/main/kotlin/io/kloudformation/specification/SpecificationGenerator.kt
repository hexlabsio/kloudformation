package io.kloudformation.specification

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.readValue
import io.kloudformation.specification.SpecificationMerger.merge
import java.lang.IllegalArgumentException
import java.net.URL
import java.util.zip.GZIPInputStream

private val jacksonObjectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
private val specificationListUrl = "https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cfn-resource-specification.html"

fun main(args: Array<String>) {
    SpecificationPoet.generate(SpecificationScraper
        .scrapeLinks(specificationListUrl)
        .map {
            (specificationName, specificationUrl) -> URL(specificationUrl).openStream().use {
            stream -> System.out.println("Downloading $specificationName from $specificationUrl")
                specificationName to if (specificationUrl.contains("gzip")) {
                    GZIPInputStream(stream).bufferedReader().readText()
                } else {
                    stream.bufferedReader().readText()
                }
            }.asSpecification()
        }
        .merge()
    )
}

private fun Pair<String, String>.asSpecification() =
    try {
        when (first) {
            "EU (Stockholm)" -> throw IllegalArgumentException("EU Stockholm is broken. IsPrivateDnsEnabled should be PrivateDnsEnabled in AWS::EC2::VPCEndpoint")
            "China (Ningxia)" -> throw IllegalArgumentException("China (Ningxia) is broken. IsPrivateDnsEnabled should be PrivateDnsEnabled in AWS::EC2::VPCEndpoint")
            else -> jacksonObjectMapper.readValue<Specification>(second)
        }
    } catch (ex: Exception) {
        System.err.println("Failed to parse $first specification")
        ex.printStackTrace()
        Specification(emptyMap(), emptyMap(), "")
    }
