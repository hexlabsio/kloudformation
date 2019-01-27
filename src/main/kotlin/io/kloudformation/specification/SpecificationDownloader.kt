package io.kloudformation.specification

import java.net.URL
import java.util.zip.GZIPInputStream

object SpecificationDownloader {
    fun downloadAll(specificationLinks: Map<String, String>) = specificationLinks.map { (specificationName, specificationUrl) ->
        URL(specificationUrl).openStream().use { stream ->
            GZIPInputStream(stream).bufferedReader().use { specificationName to it.readText() }
        }
    }.toMap()
}