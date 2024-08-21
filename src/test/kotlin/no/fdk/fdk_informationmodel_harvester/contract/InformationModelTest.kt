package no.fdk.fdk_informationmodel_harvester.contract

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.fdk.fdk_informationmodel_harvester.model.DuplicateIRI
import no.fdk.fdk_informationmodel_harvester.utils.ApiTestContext
import no.fdk.fdk_informationmodel_harvester.utils.INFO_MODEL_DBO_0
import no.fdk.fdk_informationmodel_harvester.utils.INFO_MODEL_DBO_1
import no.fdk.fdk_informationmodel_harvester.utils.INFO_MODEL_ID_0
import no.fdk.fdk_informationmodel_harvester.utils.INFO_MODEL_ID_1
import no.fdk.fdk_informationmodel_harvester.utils.TestResponseReader
import no.fdk.fdk_informationmodel_harvester.utils.apiGet
import no.fdk.fdk_informationmodel_harvester.utils.authorizedRequest
import no.fdk.fdk_informationmodel_harvester.utils.jwk.Access
import no.fdk.fdk_informationmodel_harvester.utils.jwk.JwtToken
import org.apache.jena.riot.Lang
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class InformationModelTest : ApiTestContext() {
    private val responseReader = TestResponseReader()
    private val mapper = jacksonObjectMapper()

    @Test
    fun findSpecific() {
        val response = apiGet("/informationmodels/$INFO_MODEL_ID_0?catalogrecords=true", "application/rdf+json", port)
        assertEquals(HttpStatus.OK.value(), response["status"])

        val notExpected = responseReader.parseFile("no_meta_model_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDF/JSON")

        assertFalse(notExpected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findSpecificExcludeRecords() {
        val response = apiGet("/informationmodels/$INFO_MODEL_ID_0", "application/trig", port)
        assertEquals(HttpStatus.OK.value(), response["status"])

        val expected = responseReader.parseFile("no_meta_model_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, Lang.TRIG.name)

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet("/informationmodels/123", "text/turtle", port)
        assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Nested
    internal inner class RemoveInformationModelById {

        @Test
        fun unauthorizedForNoToken() {
            val response = authorizedRequest(
                "/informationmodels/$INFO_MODEL_ID_0/remove",
                null,
                port,
                HttpMethod.POST
            )
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWithNonSysAdminRole() {
            val response = authorizedRequest(
                "/informationmodels/$INFO_MODEL_ID_0/remove",
                JwtToken(Access.ORG_WRITE).toString(),
                port,
                HttpMethod.POST
            )
            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun notFoundWhenIdNotInDB() {
            val response = authorizedRequest(
                "/informationmodels/123/remove",
                JwtToken(Access.ROOT).toString(),
                port,
                HttpMethod.POST
            )
            assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
        }

        @Test
        fun okWithSysAdminRole() {
            val response = authorizedRequest(
                "/informationmodels/$INFO_MODEL_ID_0/remove",
                JwtToken(Access.ROOT).toString(),
                port,
                HttpMethod.POST
            )
            assertEquals(HttpStatus.OK.value(), response["status"])
        }
    }

    @Nested
    internal inner class RemoveDuplicates {

        @Test
        fun unauthorizedForNoToken() {
            val body = listOf(DuplicateIRI(iriToRemove = INFO_MODEL_DBO_0.uri, iriToRetain = INFO_MODEL_DBO_1.uri))
            val response = authorizedRequest(
                "/informationmodels/remove-duplicates",
                null,
                port,
                HttpMethod.POST,
                mapper.writeValueAsString(body)
            )
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWithNonSysAdminRole() {
            val body = listOf(DuplicateIRI(iriToRemove = INFO_MODEL_DBO_0.uri, iriToRetain = INFO_MODEL_DBO_1.uri))
            val response = authorizedRequest(
                "/informationmodels/remove-duplicates",
                JwtToken(Access.ORG_WRITE).toString(),
                port,
                HttpMethod.POST,
                mapper.writeValueAsString(body)
            )
            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun badRequestWhenRemoveIRINotInDB() {
            val body = listOf(DuplicateIRI(iriToRemove = "https://123.no", iriToRetain = INFO_MODEL_DBO_1.uri))
            val response =
                authorizedRequest(
                    "/informationmodels/remove-duplicates",
                    JwtToken(Access.ROOT).toString(),
                    port,
                    HttpMethod.POST,
                    mapper.writeValueAsString(body)
                )
            assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
        }

        @Test
        fun okWithSysAdminRole() {
            val body = listOf(DuplicateIRI(iriToRemove = INFO_MODEL_DBO_0.uri, iriToRetain = INFO_MODEL_DBO_1.uri))
            val response = authorizedRequest(
                "/informationmodels/remove-duplicates",
                JwtToken(Access.ROOT).toString(),
                port,
                HttpMethod.POST,
                mapper.writeValueAsString(body)
            )
            assertEquals(HttpStatus.OK.value(), response["status"])
        }
    }

    @Nested
    internal inner class PurgeById {

        @Test
        fun unauthorizedForNoToken() {
            val response = authorizedRequest("/informationmodels/removed", null, port, HttpMethod.DELETE)
            assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
        }

        @Test
        fun forbiddenWithNonSysAdminRole() {
            val response = authorizedRequest(
                "/informationmodels/removed",
                JwtToken(Access.ORG_WRITE).toString(),
                port,
                HttpMethod.DELETE
            )
            assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
        }

        @Test
        fun badRequestWhenNotAlreadyRemoved() {
            val response = authorizedRequest(
                "/informationmodels/$INFO_MODEL_ID_1",
                JwtToken(Access.ROOT).toString(),
                port,
                HttpMethod.DELETE
            )
            assertEquals(HttpStatus.BAD_REQUEST.value(), response["status"])
        }

        @Test
        fun okWithSysAdminRole() {
            val response = authorizedRequest(
                "/informationmodels/removed",
                JwtToken(Access.ROOT).toString(),
                port,
                HttpMethod.DELETE
            )
            assertEquals(HttpStatus.NO_CONTENT.value(), response["status"])
        }

    }

}
