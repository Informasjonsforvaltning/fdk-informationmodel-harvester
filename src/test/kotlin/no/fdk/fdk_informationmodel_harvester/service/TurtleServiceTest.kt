package no.fdk.fdk_informationmodel_harvester.service

import no.fdk.fdk_informationmodel_harvester.model.CatalogTurtle
import no.fdk.fdk_informationmodel_harvester.model.FDKCatalogTurtle
import no.fdk.fdk_informationmodel_harvester.model.FDKInformationModelTurtle
import no.fdk.fdk_informationmodel_harvester.model.HarvestSourceTurtle
import no.fdk.fdk_informationmodel_harvester.model.InformationModelTurtle
import no.fdk.fdk_informationmodel_harvester.repository.CatalogTurtleRepository
import no.fdk.fdk_informationmodel_harvester.repository.FDKCatalogTurtleRepository
import no.fdk.fdk_informationmodel_harvester.repository.FDKInformationModelTurtleRepository
import no.fdk.fdk_informationmodel_harvester.repository.HarvestSourceTurtleRepository
import no.fdk.fdk_informationmodel_harvester.repository.InformationModelTurtleRepository
import no.fdk.fdk_informationmodel_harvester.utils.TestResponseReader
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@Tag("unit")
class TurtleServiceTest {
    private val harvestSourceTurtleRepository: HarvestSourceTurtleRepository = mock()
    private val catalogTurtleRepository: CatalogTurtleRepository = mock()
    private val fdkCatalogTurtleRepository: FDKCatalogTurtleRepository = mock()
    private val informationModelTurtleRepository: InformationModelTurtleRepository = mock()
    private val fdkInformationModelTurtleRepository: FDKInformationModelTurtleRepository = mock()
    private val turtleService = TurtleService(
        harvestSourceTurtleRepository,
        catalogTurtleRepository,
        fdkCatalogTurtleRepository,
        informationModelTurtleRepository,
        fdkInformationModelTurtleRepository
    )

    private val responseReader = TestResponseReader()

    @Nested
    internal inner class DBO {

        @Test
        fun equalHarvestSourceTurtle() {
            val harvestSourceTurtle = HarvestSourceTurtle(
                id = "harvestSourceTurtle",
                turtle = gzip(responseReader.readFile("harvest_response_0.ttl")),
            )

            val result = turtleService.createHarvestSourceTurtleDBO(
                uri = "harvestSourceTurtle",
                turtle = responseReader.readFile("harvest_response_0.ttl")
            )

            assertEquals(harvestSourceTurtle, result)
        }

        @Test
        fun notEqualHarvestSourceTurtle() {
            val harvestSourceTurtle = HarvestSourceTurtle(
                id = "harvestSourceTurtle",
                turtle = gzip(responseReader.readFile("harvest_response_0.ttl")),
            )

            val result = turtleService.createHarvestSourceTurtleDBO(
                uri = "harvestSourceTurtle",
                turtle = responseReader.readFile("harvest_response_1.ttl")
            )

            assertNotEquals(harvestSourceTurtle, result)
        }

        @Test
        fun equalCatalogTurtle() {
            val catalogTurtle = CatalogTurtle(
                id = "catalogTurtle",
                turtle = gzip(responseReader.readFile("no_meta_catalog_0.ttl")),
            )

            val result = turtleService.createCatalogTurtleDBO(
                id = "catalogTurtle",
                turtle = responseReader.readFile("no_meta_catalog_0.ttl")
            )

            assertEquals(catalogTurtle, result)
        }

        @Test
        fun notEqualCatalogTurtle() {
            val catalogTurtle = CatalogTurtle(
                id = "catalogTurtle",
                turtle = gzip(responseReader.readFile("no_meta_catalog_0.ttl")),
            )

            val result = turtleService.createCatalogTurtleDBO(
                id = "catalogTurtle",
                turtle = responseReader.readFile("no_meta_catalog_2.ttl")
            )

            assertNotEquals(catalogTurtle, result)
        }

        @Test
        fun equalFDKCatalogTurtle() {
            val fdkCatalogTurtle = FDKCatalogTurtle(
                id = "fdkCatalogTurtle",
                turtle = gzip(responseReader.readFile("catalog_0.ttl")),
            )

            val result = turtleService.createFDKCatalogTurtleDBO(
                id = "fdkCatalogTurtle",
                turtle = responseReader.readFile("catalog_0.ttl")
            )

            assertEquals(fdkCatalogTurtle, result)
        }

        @Test
        fun notEqualFDKCatalogTurtle() {
            val fdkCatalogTurtle = FDKCatalogTurtle(
                id = "fdkCatalogTurtle",
                turtle = gzip(responseReader.readFile("catalog_0.ttl")),
            )

            val result = turtleService.createFDKCatalogTurtleDBO(
                id = "fdkCatalogTurtle",
                turtle = responseReader.readFile("catalog_1.ttl")
            )

            assertNotEquals(fdkCatalogTurtle, result)
        }

        @Test
        fun equalInformationModelTurtle() {
            val informationModelTurtle = InformationModelTurtle(
                id = "informationModelTurtle",
                turtle = gzip(responseReader.readFile("no_meta_model_0.ttl")),
            )

            val result = turtleService.createInformationModelTurtleDBO(
                id = "informationModelTurtle",
                turtle = responseReader.readFile("no_meta_model_0.ttl")
            )

            assertEquals(informationModelTurtle, result)
        }

        @Test
        fun notEqualInformationModelTurtle() {
            val informationModelTurtle = InformationModelTurtle(
                id = "informationModelTurtle",
                turtle = gzip(responseReader.readFile("no_meta_model_0.ttl")),
            )

            val result = turtleService.createInformationModelTurtleDBO(
                id = "informationModelTurtle",
                turtle = responseReader.readFile("no_meta_model_1.ttl")
            )

            assertNotEquals(informationModelTurtle, result)
        }

        @Test
        fun equalFDKInformationModelTurtle() {
            val fdkInformationModelTurtle = FDKInformationModelTurtle(
                id = "fdkInformationModelTurtle",
                turtle = gzip(responseReader.readFile("model_0.ttl")),
            )

            val result = turtleService.createFDKInformationModelTurtleDBO(
                id = "fdkInformationModelTurtle",
                turtle = responseReader.readFile("model_0.ttl")
            )

            assertEquals(fdkInformationModelTurtle, result)
        }

        @Test
        fun notEqualFDKInformationModelTurtle() {
            val fdkInformationModelTurtle = FDKInformationModelTurtle(
                id = "fdkInformationModelTurtle",
                turtle = gzip(responseReader.readFile("model_0.ttl")),
            )

            val result = turtleService.createFDKInformationModelTurtleDBO(
                id = "fdkInformationModelTurtle",
                turtle = responseReader.readFile("model_1.ttl")
            )

            assertNotEquals(fdkInformationModelTurtle, result)
        }

    }

}
