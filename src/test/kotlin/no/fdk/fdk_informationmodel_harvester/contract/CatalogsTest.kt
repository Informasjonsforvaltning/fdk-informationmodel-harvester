package no.fdk.fdk_informationmodel_harvester.contract

import no.fdk.fdk_informationmodel_harvester.utils.ApiTestContext
import no.fdk.fdk_informationmodel_harvester.utils.CATALOG_ID_0
import no.fdk.fdk_informationmodel_harvester.utils.TestResponseReader
import no.fdk.fdk_informationmodel_harvester.utils.apiGet
import org.junit.jupiter.api.*
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
class CatalogsTest: ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Test
    fun findSpecific() {
        val response = apiGet("/catalogs/$CATALOG_ID_0?catalogrecords=true", "application/rdf+xml")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("catalog_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDFXML")

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findSpecificExcludeRecords() {
        val response = apiGet("/catalogs/$CATALOG_ID_0", "application/rdf+xml")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("no_meta_catalog_0.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "RDFXML")

        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun idDoesNotExist() {
        val response = apiGet("/catalogs/123", "text/turtle")
        Assertions.assertEquals(HttpStatus.NOT_FOUND.value(), response["status"])
    }

    @Test
    fun findAll() {
        val response = apiGet("/catalogs?catalogrecords=true", "text/turtle")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("all_catalogs.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "TURTLE")
        assertTrue(expected.isIsomorphicWith(responseModel))
    }

    @Test
    fun findAllExcludeRecords() {
        val response = apiGet("/catalogs", "text/turtle")
        Assumptions.assumeTrue(HttpStatus.OK.value() == response["status"])

        val expected = responseReader.parseFile("no_meta_all_catalogs.ttl", "TURTLE")
        val responseModel = responseReader.parseResponse(response["body"] as String, "TURTLE")
        assertTrue(expected.isIsomorphicWith(responseModel))
    }

}