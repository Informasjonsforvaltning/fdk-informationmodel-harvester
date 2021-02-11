package no.fdk.fdk_informationmodel_harvester.contract

import no.fdk.fdk_event_harvester.utils.jwk.Access
import no.fdk.fdk_event_harvester.utils.jwk.JwtToken
import no.fdk.fdk_informationmodel_harvester.utils.*
import org.junit.jupiter.api.*
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(
    properties = ["spring.profiles.active=contract-test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(initializers = [ApiTestContext.Initializer::class])
@Tag("contract")
class UpdateTest: ApiTestContext() {
    private val responseReader = TestResponseReader()

    @Test
    fun unauthorizedForNoToken() {
        val response = authorizedPost("/update/meta", null, emptyMap(), port)

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response["status"])
    }

    @Test
    fun forbiddenForNonSysAdminRole() {
        val response = authorizedPost("/update/meta", JwtToken(Access.ORG_WRITE).toString(), mapOf(Pair("X-API-KEY", "wrong-api-key")), port)

        assertEquals(HttpStatus.FORBIDDEN.value(), response["status"])
    }

    @Test
    fun noChangesWhenRunOnCorrectMeta() {
        val all = apiGet("/catalogs", "text/turtle", port)
        val catalog = apiGet("/catalogs/$CATALOG_ID_1", "text/turtle", port)
        val infoModel = apiGet("/catalogs/$INFO_MODEL_ID_1", "text/turtle", port)

        val response = authorizedPost("/update/meta", JwtToken(Access.ROOT).toString(), mapOf(Pair("X-API-KEY", "fdk-api-key")), port)

        assertEquals(HttpStatus.OK.value(), response["status"])

        val expectedAll = responseReader.parseResponse(all["body"] as String, "TURTLE")
        val expectedCatalog = responseReader.parseResponse(catalog["body"] as String, "TURTLE")
        val expectedInfoModel = responseReader.parseResponse(infoModel["body"] as String, "TURTLE")

        val allAfterUpdate = apiGet("/catalogs", "text/turtle", port)
        val catalogAfterUpdate = apiGet("/catalogs/$CATALOG_ID_1", "text/turtle", port)
        val infoModelAfterUpdate = apiGet("/catalogs/$INFO_MODEL_ID_1", "text/turtle", port)

        val actualAll = responseReader.parseResponse(allAfterUpdate["body"] as String, "TURTLE")
        val actualCatalog = responseReader.parseResponse(catalogAfterUpdate["body"] as String, "TURTLE")
        val actualInfoModel = responseReader.parseResponse(infoModelAfterUpdate["body"] as String, "TURTLE")

        assertTrue(checkIfIsomorphicAndPrintDiff(expectedAll, actualAll, "UpdateMetaAll"))
        assertTrue(checkIfIsomorphicAndPrintDiff(expectedCatalog, actualCatalog, "UpdateMetaCatalog"))
        assertTrue(checkIfIsomorphicAndPrintDiff(expectedInfoModel, actualInfoModel, "UpdateMetaInfo"))
    }

}