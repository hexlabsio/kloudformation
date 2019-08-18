package io.kloudformation.specification

import java.net.URL
import java.util.zip.GZIPInputStream

object SpecificationDownloader {
    fun downloadAll(specificationLinks: Map<String, String>) = specificationLinks.map { (specificationName, specificationUrl) ->
        URL(specificationUrl).openStream().use { stream ->
            System.out.println("Downloading $specificationUrl")
            specificationName to if(specificationUrl.contains("gzip")) {
                GZIPInputStream(stream).bufferedReader().readText()
            } else {
                stream.bufferedReader().readText()
            }
        }
    }.toMap()
}