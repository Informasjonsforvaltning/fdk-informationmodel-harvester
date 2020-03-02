package no.fdk.imcat.contract

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.imcat.dto.InformationModel
import no.fdk.imcat.utils.ApiTestContainer
import no.fdk.imcat.utils.apiGet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.hateoas.PagedResources
import org.springframework.http.HttpStatus
import kotlin.test.assertTrue

private val mapper = jacksonObjectMapper()
        .findAndRegisterModules()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("contract")
class InformationModelSearch : ApiTestContainer() {

    @Test
    fun findAllHarvestedModels() {
        val expected = 2
        val response = apiGet("/informationmodels", "application/json")
        assertEquals(HttpStatus.OK.value(), response["status"])

        val body: PagedResources<InformationModel> = mapper.readValue(response["body"] as String)
        val actual = body.metadata.totalElements
        assertTrue(actual >= expected,
                String.format("expected total information models (%d) to be greater than or equal to %d", actual, expected))
    }

    @Test
    fun searchForOrgPath() {
        val expected = 2
        val response = apiGet("/informationmodels?orgPath=STAT/87654321/12345678", "application/json")
        assertEquals(HttpStatus.OK.value(), response["status"])

        val body: PagedResources<InformationModel> = mapper.readValue(response["body"] as String)
        val actual = body.metadata.totalElements
        assertTrue(actual >= expected,
                String.format("expected total information models (%d) to be greater than or equal to %d", actual, expected))
    }

    @Test
    fun searchForSpecific() {
        val expected = 2
        val response = apiGet("/informationmodels?q=FK", "application/json")
        assertEquals(HttpStatus.OK.value(), response["status"])

        val body: PagedResources<InformationModel> = mapper.readValue(response["body"] as String)
        val actual = body.metadata.totalElements
        assertTrue(actual >= expected,
                String.format("expected total information models (%d) to be greater than or equal to %d", actual, expected))
    }

    @Test
    fun genericSearchWithAggregations() {
        val expected = 2
        val response = apiGet("/informationmodels?aggregations=orgPath,los", "application/json")
        assertEquals(HttpStatus.OK.value(), response["status"])

        val body: JsonNode = mapper.readValue(response["body"] as String)
        val actual = body.at("/page/totalElements").intValue()
        assertTrue(actual >= expected,
                String.format("expected total information models (%d) to be greater than or equal to %d", actual, expected))

        assertFalse(body.at("/aggregations/los") is MissingNode)
        assertFalse(body.at("/aggregations/orgPath") is MissingNode)
    }
}