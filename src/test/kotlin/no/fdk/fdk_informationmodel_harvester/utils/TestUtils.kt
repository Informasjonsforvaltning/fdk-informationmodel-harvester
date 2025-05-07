package no.fdk.fdk_informationmodel_harvester.utils

import no.fdk.fdk_informationmodel_harvester.rdf.createRDFResponse
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.Lang
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import java.io.BufferedReader
import java.net.URL
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.net.HttpURLConnection

private val logger = LoggerFactory.getLogger(ApiTestContext::class.java)

fun apiGet(endpoint: String, acceptHeader: String?, port: Int): Map<String, Any> {

    return try {
        val connection = URL("http://localhost:$port$endpoint").openConnection() as HttpURLConnection
        if (acceptHeader != null) connection.setRequestProperty("Accept", acceptHeader)
        connection.connect()

        if (isOK(connection.responseCode)) {
            val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            mapOf(
                "body" to responseBody,
                "header" to connection.headerFields.toString(),
                "status" to connection.responseCode
            )
        } else {
            mapOf(
                "status" to connection.responseCode,
                "header" to " ",
                "body" to " "
            )
        }
    } catch (e: Exception) {
        mapOf(
            "status" to e.toString(),
            "header" to " ",
            "body" to " "
        )
    }
}

fun authorizedRequest(
    endpoint: String,
    token: String?,
    port: Int,
    method: HttpMethod = HttpMethod.POST,
    body: String? = null,
    headers: Map<String, String> = emptyMap()
): Map<String, Any> {
    val request = RestTemplate()
    request.requestFactory = HttpComponentsClientHttpRequestFactory()
    val url = "http://localhost:$port$endpoint"
    val httpHeaders = HttpHeaders()
    token?.let { httpHeaders.setBearerAuth(it) }
    httpHeaders.contentType = MediaType.APPLICATION_JSON
    headers.forEach { httpHeaders.set(it.key, it.value) }
    val entity: HttpEntity<String> = HttpEntity(body, httpHeaders)

    return try {
        val response = request.exchange(url, method, entity, String::class.java)
        mapOf(
            "body" to response.body,
            "header" to response.headers.toString(),
            "status" to response.statusCode.value()
        )
    } catch (e: HttpClientErrorException) {
        mapOf(
            "status" to e.statusCode.value(),
            "header" to " ",
            "body" to e.toString()
        )
    } catch (e: Exception) {
        mapOf(
            "status" to e.toString(),
            "header" to " ",
            "body" to " "
        )
    }
}

private fun isOK(response: Int?): Boolean =
    if (response == null) false
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
