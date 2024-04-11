package no.fdk.fdk_informationmodel_harvester.utils

import no.fdk.fdk_informationmodel_harvester.model.CatalogMeta
import no.fdk.fdk_informationmodel_harvester.model.InformationModelMeta


val CATALOG_DBO_0 = CatalogMeta(
    uri = "${DIGDIR_PREFIX}Katalog0",
    fdkId = CATALOG_ID_0,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val INFO_MODEL_DBO_0 = InformationModelMeta(
    uri = "${DIGDIR_PREFIX}PersonOgEnhet",
    fdkId = INFO_MODEL_ID_0,
    isPartOf = "http://localhost:5050/catalogs/$CATALOG_ID_0",
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val CATALOG_DBO_1 = CatalogMeta(
    uri = "${DIGDIR_PREFIX}Katalog1",
    fdkId = CATALOG_ID_1,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)
val INFO_MODEL_DBO_1 = InformationModelMeta(
    uri = "${DIGDIR_PREFIX}AdresseModell",
    fdkId = INFO_MODEL_ID_1,
    isPartOf = "http://localhost:5050/catalogs/$CATALOG_ID_1",
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val CATALOG_DBO_2 = CatalogMeta(
    uri = "${DIGDIR_PREFIX_2}Katalog2",
    fdkId = CATALOG_ID_2,
    issued = TEST_HARVEST_DATE.timeInMillis,
    modified = TEST_HARVEST_DATE.timeInMillis
)

val INFO_MODEL_META_2 = InformationModelMeta(
    uri="https://raw.github.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#AltMuligModell",
    fdkId="0bf6b09f-e1c0-3415-bba0-7ff2edada89d",
    isPartOf="http://localhost:5050/catalogs/f25c939d-0722-3aa3-82b1-eaa457086444",
    issued=1601903739831, modified=1601903739831)

val INFO_MODEL_META_3 = InformationModelMeta(
    uri="https://raw.github.com/Informasjonsforvaltning/model-publisher/master/src/model/model-catalog.ttl#Diversemodell",
    fdkId="bcbe6738-85f6-388c-afcc-ef1fafd7cc45",
    isPartOf="http://localhost:5050/catalogs/f25c939d-0722-3aa3-82b1-eaa457086444",
    issued=1601903739831, modified=1601903739831)
