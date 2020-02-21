package no.fdk.imcat.contract

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.imcat.dto.InformationModelDto
import no.fdk.imcat.model.InformationModel
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

private val mapper = jacksonObjectMapper()
        .findAndRegisterModules()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

@Disabled
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("contract")
class InformationModelSearch : ApiTestContainer() {

    @Test
    fun findAllHarvestedModelsFromV2() {
        val response = apiGet("/informationmodels/v2", "application/json")
        assertEquals(HttpStatus.OK.value(), response["status"])

        val body: PagedResources<InformationModelDto> = mapper.readValue(response["body"] as String)
        assertEquals(4, body.metadata.totalElements)
    }

    @Test
    fun findAllHarvestedModels() {
        val response = apiGet("/informationmodels", "application/json")
        assertEquals(HttpStatus.OK.value(), response["status"])

        val body: PagedResources<InformationModel> = mapper.readValue(response["body"] as String)
        assertEquals(4, body.metadata.totalElements)
    }

    @Test
    fun searchForOrgPath() {
        val response = apiGet("/informationmodels?orgPath=STAT/87654321/12345678", "application/json")
        assertEquals(HttpStatus.OK.value(), response["status"])

        val body: PagedResources<InformationModel> = mapper.readValue(response["body"] as String)
        assertEquals(3, body.metadata.totalElements)
    }

    @Test
    fun searchForSpecific() {
        val response = apiGet("/informationmodels?q=A04", "application/json")
        assertEquals(HttpStatus.OK.value(), response["status"])

        val body: PagedResources<InformationModel> = mapper.readValue(response["body"] as String)
        assertEquals(1, body.metadata.totalElements)
    }

    @Test
    fun genericSearchWithAggregations() {
        val response = apiGet("/informationmodels?aggregations=orgPath,los", "application/json")
        assertEquals(HttpStatus.OK.value(), response["status"])

        val body: JsonNode = mapper.readValue(response["body"] as String)
        assertEquals(4, body.at("/page/totalElements").intValue())
        assertFalse(body.at("/aggregations/los") is MissingNode)
        assertFalse(body.at("/aggregations/orgPath") is MissingNode)
    }
}