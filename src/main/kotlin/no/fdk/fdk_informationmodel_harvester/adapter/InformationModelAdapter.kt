package no.fdk.fdk_informationmodel_harvester.adapter

import no.fdk.fdk_informationmodel_harvester.harvester.HarvestException
import no.fdk.fdk_informationmodel_harvester.model.HarvestDataSource
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

private const val TEN_MINUTES = 600000

@Service
class InformationModelAdapter {

    fun getInformationModels(source: HarvestDataSource): String {
        with(URL(source.url).openConnection() as HttpURLConnection) {
            try {
                setRequestProperty("Accept", source.acceptHeaderValue)
                connectTimeout = TEN_MINUTES
                readTimeout = TEN_MINUTES

                return if (responseCode != HttpStatus.OK.value()) {
                    throw HarvestException("${source.url} responded with ${responseCode}, harvest was be aborted")
                } else {
                    inputStream.bufferedReader()
                        .use(BufferedReader::readText)
                }

            } finally {
                disconnect()
            }
        }
    }

}
