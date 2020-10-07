package no.fdk.fdk_informationmodel_harvester.utils

import no.fdk.fdk_informationmodel_harvester.model.HarvestDataSource
import org.testcontainers.shaded.com.google.common.collect.ImmutableMap
import java.util.*

const val API_PORT = 8080
const val API_TEST_PORT = 5555
const val LOCAL_SERVER_PORT = 5000

const val API_TEST_URI = "http://localhost:$API_TEST_PORT"
const val WIREMOCK_TEST_URI = "http://localhost:$LOCAL_SERVER_PORT"

const val MONGO_USER = "testuser"
const val MONGO_PASSWORD = "testpassword"
const val MONGO_PORT = 27017
const val MONGO_COLLECTION = "informationModelHarvester"

val MONGO_ENV_VALUES: Map<String, String> = ImmutableMap.of(
    "MONGO_INITDB_ROOT_USERNAME", MONGO_USER,
    "MONGO_INITDB_ROOT_PASSWORD", MONGO_PASSWORD
)

const val DIGDIR_PREFIX = "https://raw.githubusercontent.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#"
const val DIGDIR_PREFIX_2 = "https://raw.github.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#"

const val INFO_MODEL_ID_0 = "409c97dd-57e0-3a29-b5a3-023733cf5064"
const val INFO_MODEL_ID_1 = "1f44a866-2cbd-3b80-bf72-cccb99965e25"
const val CATALOG_ID_0 = "e5b2ad5e-078b-3aea-af04-6051c2b0244b"
const val CATALOG_ID_1 = "3edd0561-326b-303d-9dcb-1966ef7c63eb"
const val CATALOG_ID_2 = "f25c939d-0722-3aa3-82b1-eaa457086444"

val TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 9, 5).setTimeOfDay(13, 15, 39, 831).build()
val NEW_TEST_HARVEST_DATE: Calendar = Calendar.Builder().setTimeZone(TimeZone.getTimeZone("UTC")).setDate(2020, 9, 15).setTimeOfDay(11, 52, 16, 122).build()

val TEST_HARVEST_SOURCE = HarvestDataSource(
    url = "$WIREMOCK_TEST_URI/harvest",
    acceptHeaderValue = "text/turtle",
    dataType = "informationmodel",
    dataSourceType = "DCAT-AP-NO"
)

val TEST_HARVEST_SOURCE_2 = HarvestDataSource(
    url = "$WIREMOCK_TEST_URI/harvest2",
    acceptHeaderValue = "text/turtle",
    dataType = "informationmodel",
    dataSourceType = "DCAT-AP-NO"
)

val ERROR_HARVEST_SOURCE = HarvestDataSource(
    url = "$WIREMOCK_TEST_URI/error-harvest",
    acceptHeaderValue = "text/turtle",
    dataType = "informationmodel",
    dataSourceType = "DCAT-AP-NO"
)