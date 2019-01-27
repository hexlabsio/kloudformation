package io.kloudformation.specification

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.module.kotlin.readValue

private val jacksonObjectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
private val specificationListUrl = "https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/cfn-resource-specification.html"

fun main(args: Array<String>) {
    val scrapedLinks = SpecificationScraper.scrapeLinks(specificationListUrl)
    val downloadedSpecificationStrings = SpecificationDownloader.downloadAll(scrapedLinks)
    val parsedSpecifications = downloadedSpecificationStrings.map {
        try {
            jacksonObjectMapper.readValue<Specification>(it.value)
        } catch (ex: Exception) {
            System.err.println("Failed to parse ${it.key} specification")
            Specification(emptyMap(), emptyMap(), "")
        }
    }
    val mergedSpecification = SpecificationMerger.merge(parsedSpecifications)
    SpecificationPoet.generate(mergedSpecification)
}