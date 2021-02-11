package no.fdk.fdk_informationmodel_harvester.contract

import no.fdk.fdk_informationmodel_harvester.utils.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class CatalogsTest: ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Test
    fun findSpecific() {
        val response = apiGet("/catalogs/$CATALOG_ID_0?catalogrecords=true", "application/rdf+xml", port)
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val notExpected = responseReader.parseFile("no_meta_catalog_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDFXML")

        assertFalse(notExpected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findSpecificExcludeRecords() {
        val response = apiGet("/catalogs/$CATALOG_ID_0", "application/rdf+xml", port)
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("no_meta_catalog_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDFXML")

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet("/catalogs/123", "text/turtle", port)
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Test
    fun findAll() {
        val response = apiGet("/catalogs?catalogrecords=true", "text/turtle", port)
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val notExpected = responseReader.parseFile("no_meta_all_catalogs.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "TURTLE")
        assertFalse(notExpected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findAllExcludeRecords() {
        val response = apiGet("/catalogs", "text/turtle", port)
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("no_meta_all_catalogs.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "TURTLE")
        assertTrue(expected.isIsomorphicWith(responseModel))
    }

}