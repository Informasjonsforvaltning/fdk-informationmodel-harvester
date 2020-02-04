package no.fdk.imcat.contract

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.fdk.imcat.model.InformationModel
import no.fdk.imcat.utils.ApiTestContainer
import no.fdk.imcat.utils.apiGet
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.hateoas.PagedResources
import org.springframework.http.HttpStatus

private val mapper = jacksonObjectMapper()
    .findAndRegisterModules()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("contract")
class InformationModelSearch : ApiTestContainer() {

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

}