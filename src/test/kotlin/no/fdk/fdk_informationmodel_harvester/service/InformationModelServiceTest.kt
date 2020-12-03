package no.fdk.fdk_informationmodel_harvester.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import no.fdk.fdk_informationmodel_harvester.model.MiscellaneousTurtle
import no.fdk.fdk_informationmodel_harvester.model.NO_FDK_UNION_ID
import no.fdk.fdk_informationmodel_harvester.model.UNION_ID
import no.fdk.fdk_informationmodel_harvester.rdf.JenaType
import no.fdk.fdk_informationmodel_harvester.repository.CatalogRepository
import no.fdk.fdk_informationmodel_harvester.repository.InformationModelRepository
import no.fdk.fdk_informationmodel_harvester.repository.MiscellaneousRepository
import no.fdk.fdk_informationmodel_harvester.utils.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Tag("unit")
class InformationModelServiceTest {
    private val catalogRepository: CatalogRepository = mock()
    private val modelRepository: InformationModelRepository = mock()
    private val miscRepository: MiscellaneousRepository = mock()
    private val modelService = InformationModelService(catalogRepository, modelRepository, miscRepository)

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class AllCatalogs {

        @Test
        fun responseIsometricWithEmptyModelForEmptyDB() {
            whenever(miscRepository.findById(UNION_ID))
                .thenReturn(Optional.empty())
            whenever(miscRepository.findById(NO_FDK_UNION_ID))
                .thenReturn(Optional.empty())

            val expected = responseReader.parseResponse("", "TURTLE")

            val responseTurtle = modelService.getAll(JenaType.TURTLE, true)
            val responseJsonLD = modelService.getAll(JenaType.JSON_LD, false)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseJsonLD, "JSON-LD")))
        }

        @Test
        fun getAllHandlesTurtleAndOtherRDF() {
            val allCatalogs = MiscellaneousTurtle(
                id = UNION_ID,
                isHarvestedSource = false,
                turtle = gzip(javaClass.classLoader.getResource("all_catalogs.ttl")!!.readText())
            )

            whenever(miscRepository.findById(UNION_ID))
                .thenReturn(Optional.of(allCatalogs))

            val expected = responseReader.parseFile("all_catalogs.ttl", "TURTLE")

            val responseTurtle = modelService.getAll(JenaType.TURTLE, true)
            val responseN3 = modelService.getAll(JenaType.N3, true)
            val responseNTriples = modelService.getAll(JenaType.NTRIPLES, true)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseN3, "N3")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseNTriples, "N-TRIPLES")))
        }

        @Test
        fun getAllHarvestedHandlesTurtleAndOtherRDF() {
            val allCatalogs = MiscellaneousTurtle(
                id = NO_FDK_UNION_ID,
                isHarvestedSource = false,
                turtle = gzip(javaClass.classLoader.getResource("no_meta_all_catalogs.ttl")!!.readText())
            )

            whenever(miscRepository.findById(NO_FDK_UNION_ID))
                .thenReturn(Optional.of(allCatalogs))

            val expected = responseReader.parseFile("no_meta_all_catalogs.ttl", "TURTLE")

            val responseTurtle = modelService.getAll(JenaType.TURTLE, false)
            val responseN3 = modelService.getAll(JenaType.N3, false)
            val responseNTriples = modelService.getAll(JenaType.NTRIPLES, false)

            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseTurtle, "TURTLE")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseN3, "N3")))
            assertTrue(expected.isIsomorphicWith(responseReader.parseResponse(responseNTriples, "N-TRIPLES")))
        }

    }

    @Nested
    internal inner class CatalogById {

        @Test
        fun responseIsNullWhenNoCatalogIsFound() {
            whenever(catalogRepository.findOneByFdkId("123"))
                .thenReturn(null)

            val response = modelService.getCatalogById("123", JenaType.TURTLE, true)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(catalogRepository.findOneByFdkId(CATALOG_ID_0))
                .thenReturn(CATALOG_DBO_0)

            val responseTurtle = modelService.getCatalogById(CATALOG_ID_0, JenaType.TURTLE, true)
            val responseJsonRDF = modelService.getCatalogById(CATALOG_ID_0, JenaType.RDF_JSON, false)

            val expectedWithMeta = responseReader.parseFile("catalog_0.ttl", "TURTLE")
            val expectedNoMeta = responseReader.parseFile("no_meta_catalog_0.ttl", "TURTLE")

            assertTrue(expectedWithMeta.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expectedNoMeta.isIsomorphicWith(responseReader.parseResponse(responseJsonRDF!!, "RDF/JSON")))
        }

    }

    @Nested
    internal inner class InformationModelById {

        @Test
        fun responseIsNullWhenNoModelIsFound() {
            whenever(modelRepository.findOneByFdkId("123"))
                .thenReturn(null)

            val response = modelService.getInformationModelById("123", JenaType.TURTLE, true)

            assertNull(response)
        }

        @Test
        fun responseIsIsomorphicWithExpectedModel() {
            whenever(modelRepository.findOneByFdkId(INFO_MODEL_ID_0))
                .thenReturn(INFO_MODEL_DBO_0)

            val responseTurtle = modelService.getInformationModelById(INFO_MODEL_ID_0, JenaType.TURTLE, false)
            val responseRDFXML = modelService.getInformationModelById(INFO_MODEL_ID_0, JenaType.RDF_XML, true)

            val expectedWithMeta = responseReader.parseFile("model_0.ttl", "TURTLE")
            val expectedNoMeta = responseReader.parseFile("no_meta_model_0.ttl", "TURTLE")

            assertTrue(expectedNoMeta.isIsomorphicWith(responseReader.parseResponse(responseTurtle!!, "TURTLE")))
            assertTrue(expectedWithMeta.isIsomorphicWith(responseReader.parseResponse(responseRDFXML!!, "RDF/XML")))
        }

    }
}