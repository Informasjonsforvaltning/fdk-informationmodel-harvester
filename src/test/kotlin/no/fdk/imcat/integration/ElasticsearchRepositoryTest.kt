package no.fdk.imcat.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import no.fdk.imcat.ImcatApiApplication
import no.fdk.imcat.controller.InformationModelSearchController
import no.fdk.imcat.utils.ElasticsearchTestContainer
import no.fdk.imcat.utils.PropertyOverrideContextInitializer
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.io.FileReader
import kotlin.test.assertEquals

@ExtendWith(SpringExtension::class)
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = [PropertyOverrideContextInitializer::class], classes = [ImcatApiApplication::class])
class ElasticsearchRepositoryTest : ElasticsearchTestContainer() {
    private val mapper = ObjectMapper()
    private lateinit var expected: JsonNode

    @Autowired
    private lateinit var informationModelSearchController: InformationModelSearchController

    @BeforeAll
    fun init() {
        val reader = FileReader("src/test/resources/schemas.json")
        reader.use { expected = mapper.readTree(reader.buffered().readText()) }
    }

    @Test
    fun `search all models`() {
        val query = ""
        val orgPath = ""
        val harvestSourceUri = ""
        val sortField = ""
        val sortDirection = ""
        val losThemes: Set<String> = emptySet()
        val aggregations: Set<String> = emptySet()
        val returnFields: Array<String> = emptyArray()
        val conceptUris: Set<String> = emptySet()
        val pageable = PageRequest.of(0, 10)

        val models = informationModelSearchController.searchNew(query, orgPath, harvestSourceUri, losThemes, aggregations, returnFields, sortField, sortDirection, conceptUris, pageable).content
        assertEquals(4, models.size, "size mismatch")
        models.forEach { assertEquals(1, it.title.size) }
    }
}
