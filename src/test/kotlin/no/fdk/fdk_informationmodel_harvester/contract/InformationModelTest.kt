package no.fdk.fdk_informationmodel_harvester.contract

import no.fdk.fdk_informationmodel_harvester.utils.ApiTestContext
import no.fdk.fdk_informationmodel_harvester.utils.INFO_MODEL_ID_0
import no.fdk.fdk_informationmodel_harvester.utils.TestResponseReader
import no.fdk.fdk_informationmodel_harvester.utils.apiGet
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class InformationModelTest: ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Test
    fun findSpecific() {
        val response = apiGet("/informationmodels/$INFO_MODEL_ID_0?catalogrecords=true", "application/rdf+json")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val notExpected = responseReader.parseFile("no_meta_model_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDF/JSON")

        assertFalse(notExpected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findSpecificExcludeRecords() {
        val response = apiGet("/informationmodels/$INFO_MODEL_ID_0", "application/rdf+json")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("no_meta_model_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDF/JSON")

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet("/dataservices/123", "text/turtle")
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

}