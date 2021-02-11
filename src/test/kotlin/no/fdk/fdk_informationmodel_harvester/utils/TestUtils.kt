package no.fdk.fdk_informationmodel_harvester.utils

import no.fdk.fdk_informationmodel_harvester.rdf.createRDFResponse
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.net.URL
import org.springframework.http.HttpStatus
import java.net.HttpURLConnection

private val logger = LoggerFactory.getLogger(ApiTestContext::class.java)

fun apiGet(endpoint: String, acceptHeader: String?): Map<String,Any> {

    return try {
        val connection = URL("$API_TEST_URI$endpoint").openConnection() as HttpURLConnection
        if(acceptHeader != null) connection.setRequestProperty("Accept", acceptHeader)
        connection.connect()

        if(isOK(connection.responseCode)) {
            val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            mapOf(
                "body"   to responseBody,
                "header" to connection.headerFields.toString(),
                "status" to connection.responseCode)
        } else {
            mapOf(
                "status" to connection.responseCode,
                "header" to " ",
                "body"   to " "
            )
        }
    } catch (e: Exception) {
        mapOf(
            "status" to e.toString(),
            "header" to " ",
            "body"   to " "
        )
    }
}

fun authorizedPost(endpoint: String, token: String?, headers: Map<String, String>): Map<String,Any> {

    return try {
        val connection = URL("$API_TEST_URI$endpoint").openConnection() as HttpURLConnection
        headers.forEach { (key, value) -> connection.setRequestProperty(key, value) }
        if(!token.isNullOrEmpty()) connection.setRequestProperty("Authorization", "Bearer $token")
        connection.requestMethod = "POST"
        connection.connect()

        if(isOK(connection.responseCode)) {
            val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            mapOf(
                "body"   to responseBody,
                "header" to connection.headerFields.toString(),
                "status" to connection.responseCode)
        } else {
            mapOf(
                "status" to connection.responseCode,
                "header" to " ",
                "body"   to " "
            )
        }
    } catch (e: Exception) {
        mapOf(
            "status" to e.toString(),
            "header" to " ",
            "body"   to " "
        )
    }
}

fun harvestCompleted(): Boolean {
    return try {
        val connection = URL("$API_TEST_URI/count").openConnection() as HttpURLConnection
        connection.connect()

        if(isOK(connection.responseCode)) {
            val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            return responseBody.toInt() > 0
        } else {
            false
        }
    } catch (e: Exception) {
        false
    }
}

private fun isOK(response: Int?): Boolean =
    if(response == null) false
    else HttpStatus.resolve(response)?.is2xxSuccessful == true

fun checkIfIsomorphicAndPrintDiff(actual: Model, expected: Model, name: String): Boolean {
    val isIsomorphic = actual.isIsomorphicWith(expected)

    if (!isIsomorphic) {
        val missing = actual.difference(expected).createRDFResponse(Lang.TURTLE)
        val nonExpected = expected.difference(actual).createRDFResponse(Lang.TURTLE)

        if (missing.isNotEmpty()) {
            logger.error("missing nodes in $name:")
            logger.error(missing)
        }
        if (nonExpected.isNotEmpty()) {
            logger.error("non expected nodes in $name:")
            logger.error(nonExpected)
        }
    }
    return isIsomorphic
}
