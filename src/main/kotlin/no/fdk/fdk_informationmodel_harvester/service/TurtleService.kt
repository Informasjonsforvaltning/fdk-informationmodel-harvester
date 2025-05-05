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
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private const val UNION_ID = "union-graph"

@Service
class TurtleService (
    private val harvestSourceTurtleRepository: HarvestSourceTurtleRepository,
    private val catalogTurtleRepository: CatalogTurtleRepository,
    private val fdkCatalogTurtleRepository: FDKCatalogTurtleRepository,
    private val informationModelTurtleRepository: InformationModelTurtleRepository,
    private val fdkInformationModelTurtleRepository: FDKInformationModelTurtleRepository
) {

    fun saveAsCatalogUnion(turtle: String, withRecords: Boolean) {
        if (withRecords) fdkCatalogTurtleRepository.save(createFDKCatalogTurtleDBO(UNION_ID, turtle))
        else catalogTurtleRepository.save(createCatalogTurtleDBO(UNION_ID, turtle))
    }

    fun findCatalogUnion(withRecords: Boolean): String? =
        if (withRecords) fdkCatalogTurtleRepository.findByIdOrNull(UNION_ID)
            ?.turtle
            ?.let { ungzip(it) }
        else catalogTurtleRepository.findByIdOrNull(UNION_ID)
            ?.turtle
            ?.let { ungzip(it) }

    fun saveCatalog(fdkId: String, turtle: String, withRecords: Boolean) {
        if (withRecords) fdkCatalogTurtleRepository.save(createFDKCatalogTurtleDBO(fdkId, turtle))
        else catalogTurtleRepository.save(createCatalogTurtleDBO(fdkId, turtle))
    }

    fun findCatalog(fdkId: String, withRecords: Boolean): String? =
        if (withRecords) fdkCatalogTurtleRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }
        else catalogTurtleRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }

    fun saveInformationModel(fdkId: String, turtle: String, withRecords: Boolean) {
        if (withRecords) fdkInformationModelTurtleRepository
            .save(createFDKInformationModelTurtleDBO(fdkId, turtle))
        else informationModelTurtleRepository
            .save(createInformationModelTurtleDBO(fdkId, turtle))
    }

    fun findInformationModel(fdkId: String, withRecords: Boolean): String? =
        if (withRecords) fdkInformationModelTurtleRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }
        else informationModelTurtleRepository.findByIdOrNull(fdkId)
            ?.turtle
            ?.let { ungzip(it) }

    fun saveAsHarvestSource(uri: String, turtle: String) {
        harvestSourceTurtleRepository.save(createHarvestSourceTurtleDBO(uri, turtle))
    }

    fun findHarvestSource(uri: String): String? =
        harvestSourceTurtleRepository.findByIdOrNull(uri)
            ?.turtle
            ?.let { ungzip(it) }

    fun deleteTurtleFiles(fdkId: String) {
        informationModelTurtleRepository.deleteById(fdkId)
        fdkInformationModelTurtleRepository.deleteById(fdkId)
    }

    fun createInformationModelTurtleDBO(id: String, turtle: String): InformationModelTurtle =
        InformationModelTurtle(
            id = id,
            turtle = gzip(turtle)
        )

    fun createFDKInformationModelTurtleDBO(id: String, turtle: String): FDKInformationModelTurtle =
        FDKInformationModelTurtle(
            id = id,
            turtle = gzip(turtle)
        )

    fun createCatalogTurtleDBO(id: String, turtle: String): CatalogTurtle =
        CatalogTurtle(
            id = id,
            turtle = gzip(turtle)
        )

    fun createFDKCatalogTurtleDBO(id: String, turtle: String): FDKCatalogTurtle =
        FDKCatalogTurtle(
            id = id,
            turtle = gzip(turtle)
        )

    fun createHarvestSourceTurtleDBO(uri: String, turtle: String): HarvestSourceTurtle =
        HarvestSourceTurtle(
            id = uri,
            turtle = gzip(turtle)
        )

}

fun gzip(content: String): String {
    val bos = ByteArrayOutputStream()
    GZIPOutputStream(bos).bufferedWriter(Charsets.UTF_8).use { it.write(content) }
    return Base64.getEncoder().encodeToString(bos.toByteArray())
}

fun ungzip(base64Content: String): String {
    val content = Base64.getDecoder().decode(base64Content)
    return GZIPInputStream(content.inputStream())
        .bufferedReader(Charsets.UTF_8)
        .use { it.readText() }
}
