package no.fdk.fdk_informationmodel_harvester.contract

import no.fdk.fdk_informationmodel_harvester.utils.*
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class UpdateTest: ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Test
    fun forbiddenWhenNoApiKey() {
        val response = apiPost("/update/meta", emptyMap())

        assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
    }

    @Test
    fun forbiddenWhenWrongApiKey() {
        val response = apiPost("/update/meta", mapOf(Pair("X-API-KEY", "wrong-api-key")))

        assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
    }

    @Test
    fun noChangesWhenRunOnCorrectMeta() {
        val all = apiGet("/catalogs", "text/turtle")
        val catalog = apiGet("/catalogs/$CATALOG_ID_1", "text/turtle")
        val infoModel = apiGet("/catalogs/$INFO_MODEL_ID_1", "text/turtle")

        val response = apiPost("/update/meta", mapOf(Pair("X-API-KEY", "fdk-api-key")))

        assertEquals(HttpStatus.OK.value(), response["status"])

        val expectedAll = responseReader.parseResponse(all["body"] as String, "TURTLE")
        val expectedCatalog = responseReader.parseResponse(catalog["body"] as String, "TURTLE")
        val expectedInfoModel = responseReader.parseResponse(infoModel["body"] as String, "TURTLE")

        val allAfterUpdate = apiGet("/catalogs", "text/turtle")
        val catalogAfterUpdate = apiGet("/catalogs/$CATALOG_ID_1", "text/turtle")
        val infoModelAfterUpdate = apiGet("/catalogs/$INFO_MODEL_ID_1", "text/turtle")

        val actualAll = responseReader.parseResponse(allAfterUpdate["body"] as String, "TURTLE")
        val actualCatalog = responseReader.parseResponse(catalogAfterUpdate["body"] as String, "TURTLE")
        val actualInfoModel = responseReader.parseResponse(infoModelAfterUpdate["body"] as String, "TURTLE")

        assertTrue(checkIfIsomorphicAndPrintDiff(expectedAll, actualAll, "UpdateMetaAll"))
        assertTrue(checkIfIsomorphicAndPrintDiff(expectedCatalog, actualCatalog, "UpdateMetaCatalog"))
        assertTrue(checkIfIsomorphicAndPrintDiff(expectedInfoModel, actualInfoModel, "UpdateMetaInfo"))
    }

}