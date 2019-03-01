package io.kloudformation.specification

import org.jsoup.Jsoup

object SpecificationScraper {
    fun scrapeLinks(specificationListUrl: String): Map<String, String> = Jsoup.connect(specificationListUrl).get()
            .select(".table-contents tr td p").toList()
            .chunked(4)
            .map { it[0].text() to it[2].child(0).attr("href") }
            .toMap()
}