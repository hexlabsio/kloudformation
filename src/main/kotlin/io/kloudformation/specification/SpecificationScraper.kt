package io.kloudformation.specification

import org.jsoup.Jsoup

object SpecificationScraper {
    fun scrapeLinks(specificationListUrl: String): Map<String, String> = Jsoup.connect(specificationListUrl).get()
            .select(".table-contents tr td p").toList()
            .chunked(3)
            .map { it[0].text() to it[1].child(0).attr("href") }
            .toMap()
}