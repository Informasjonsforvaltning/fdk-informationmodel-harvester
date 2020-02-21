package no.fdk.imcat.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import java.io.File

private val mockserver = WireMockServer(LOCAL_SERVER_PORT)

fun startMockServer() {
    if(!mockserver.isRunning) {
        mockserver.stubFor(get(urlEqualTo("/ping"))
                .willReturn(aResponse()
                        .withStatus(200))
        )

        mockserver.stubFor(get(urlEqualTo("/api/registration/apis"))
            .willReturn(okJson(File("src/test/resources/api-reg.json").readText())))

        mockserver.stubFor(get(urlEqualTo("/organizations/985399077"))
            .willReturn(okJson(File("src/test/resources/org-0.json").readText())))

        mockserver.stubFor(get(urlEqualTo("/organizations/974760223"))
            .willReturn(okJson(File("src/test/resources/org-1.json").readText())))

        mockserver.stubFor(get(urlEqualTo("/reference-data/los"))
                .willReturn(okJson(File("src/test/resources/ref.json").readText())))

        mockserver.start()
    }
}

fun stopMockServer() {

    if (mockserver.isRunning) mockserver.stop()

}