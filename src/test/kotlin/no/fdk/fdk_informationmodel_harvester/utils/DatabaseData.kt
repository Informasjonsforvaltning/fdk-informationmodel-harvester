package no.fdk.fdk_informationmodel_harvester.utils

import no.fdk.fdk_informationmodel_harvester.model.CatalogDBO
import no.fdk.fdk_informationmodel_harvester.model.InformationModelDBO
import no.fdk.fdk_informationmodel_harvester.model.MiscellaneousTurtle
import no.fdk.fdk_informationmodel_harvester.model.UNION_ID
import no.fdk.fdk_informationmodel_harvester.service.gzip
import org.bson.Document

private val responseReader = TestResponseReader()


val CATALOG_DBO_0 = CatalogDBO(
    uri = "${DIGDIR_PREFIX}Katalog0",
    fdkId = CATALOG_ID_0,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis,
    turtleHarvested = gzip(responseReader.readFile("harvest_response_0.ttl")),
    turtleCatalog = gzip(responseReader.readFile("catalog_0.ttl"))
)
val INFO_MODEL_DBO_0 = InformationModelDBO(
    uri = "${DIGDIR_PREFIX}PersonOgEnhet",
    fdkId = INFO_MODEL_ID_0,
    isPartOf = "http://localhost:5000/catalogs/$CATALOG_ID_0",
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis,
    turtleHarvested = gzip(responseReader.readFile("no_meta_model_0.ttl")),
    turtleInformationModel= gzip(responseReader.readFile("model_0.ttl"))
)

val CATALOG_DBO_1 = CatalogDBO(
    uri = "${DIGDIR_PREFIX}Katalog1",
    fdkId = CATALOG_ID_1,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis,
    turtleHarvested = gzip(responseReader.readFile("harvest_response_1.ttl")),
    turtleCatalog = gzip(responseReader.readFile("catalog_1.ttl"))
)
val INFO_MODEL_DBO_1 = InformationModelDBO(
    uri = "${DIGDIR_PREFIX}AdresseModell",
    fdkId = INFO_MODEL_ID_1,
    isPartOf = "http://localhost:5000/catalogs/$CATALOG_ID_1",
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis,
    turtleHarvested = gzip(responseReader.readFile("no_meta_model_1.ttl")),
    turtleInformationModel = gzip(responseReader.readFile("model_1.ttl"))
)

val CATALOG_DBO_2 = CatalogDBO(
    uri = "${DIGDIR_PREFIX_2}Katalog2",
    fdkId = CATALOG_ID_2,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis,
    turtleHarvested = gzip(responseReader.readFile("harvest_response_2.ttl")),
    turtleCatalog = gzip(responseReader.readFile("catalog_2.ttl"))
)

val UNION_DATA = MiscellaneousTurtle(
    id = UNION_ID,
    isHarvestedSource = false,
    turtle = gzip(responseReader.readFile("all_catalogs.ttl"))
)

val HARVEST_DBO_0 = MiscellaneousTurtle(
    id = TEST_HARVEST_SOURCE.url!!,
    isHarvestedSource = true,
    turtle = gzip(responseReader.readFile("harvest_response_0.ttl"))
)

val HARVEST_DBO_1 = MiscellaneousTurtle(
    id = "$WIREMOCK_TEST_URI/harvest1",
    isHarvestedSource = true,
    turtle = gzip(responseReader.readFile("harvest_response_1.ttl"))
)

val HARVEST_DBO_2 = MiscellaneousTurtle(
    id = "$WIREMOCK_TEST_URI/harvest2",
    isHarvestedSource = true,
    turtle = gzip(responseReader.readFile("harvest_response_2.ttl"))
)

fun miscDBPopulation(): List<Document> =
    listOf(UNION_DATA, HARVEST_DBO_0, HARVEST_DBO_1)
        .map { it.mapDBO() }

fun catalogDBPopulation(): List<Document> =
    listOf(CATALOG_DBO_0, CATALOG_DBO_1)
        .map { it.mapDBO() }

fun serviceDBPopulation(): List<Document> =
    listOf(INFO_MODEL_DBO_0, INFO_MODEL_DBO_1)
        .map { it.mapDBO() }

private fun CatalogDBO.mapDBO(): Document =
    Document()
        .append("_id", uri)
        .append("fdkId", fdkId)
        .append("issued", issued)
        .append("modified", modified)
        .append("turtleHarvested", turtleHarvested)
        .append("turtleCatalog", turtleCatalog)

private fun InformationModelDBO.mapDBO(): Document =
    Document()
        .append("_id", uri)
        .append("fdkId", fdkId)
        .append("isPartOf", isPartOf)
        .append("issued", issued)
        .append("modified", modified)
        .append("turtleHarvested", turtleHarvested)
        .append("turtleInformationModel", turtleInformationModel)

private fun MiscellaneousTurtle.mapDBO(): Document =
    Document()
        .append("_id", id)
        .append("isHarvestedSource", isHarvestedSource)
        .append("turtle", turtle)
