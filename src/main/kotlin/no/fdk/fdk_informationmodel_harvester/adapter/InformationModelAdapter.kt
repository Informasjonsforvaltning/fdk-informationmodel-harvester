package no.fdk.fdk_informationmodel_harvester.adapter

import no.fdk.fdk_informationmodel_harvester.harvester.HarvestException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI

private const val TEN_MINUTES = 600000

@Service
class InformationModelAdapter {

    fun getInformationModels(url: String, acceptHeader: String): String {
        with(URI(url).toURL().openConnection() as HttpURLConnection) {
            try {
                setRequestProperty("Accept", acceptHeader)
                connectTimeout = TEN_MINUTES
                readTimeout = TEN_MINUTES

                return if (responseCode != HttpStatus.OK.value()) {
                    throw HarvestException("$url responded with ${responseCode}, harvest was be aborted")
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
