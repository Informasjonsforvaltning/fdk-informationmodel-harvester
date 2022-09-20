package no.fdk.fdk_informationmodel_harvester.harvester

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import no.fdk.fdk_informationmodel_harvester.adapter.HarvestAdminAdapter
import no.fdk.fdk_informationmodel_harvester.model.HarvestAdminParameters
import no.fdk.fdk_informationmodel_harvester.rabbit.RabbitMQPublisher
import no.fdk.fdk_informationmodel_harvester.service.UpdateService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Calendar
import javax.annotation.PostConstruct

private val LOGGER = LoggerFactory.getLogger(HarvesterActivity::class.java)

@Service
class HarvesterActivity(
    private val harvestAdminAdapter: HarvestAdminAdapter,
    private val harvester: InformationModelHarvester,
    private val publisher: RabbitMQPublisher,
    private val updateService: UpdateService
) : CoroutineScope by CoroutineScope(Dispatchers.Default) {

    private val activitySemaphore = Semaphore(1)

    @PostConstruct
    private fun fullHarvestOnStartup() = initiateHarvest(HarvestAdminParameters(null, null, null), false)

    fun initiateHarvest(params: HarvestAdminParameters, forceUpdate: Boolean) {
        if (params.harvestAllModels()) LOGGER.debug("starting harvest of all information models, force update: $forceUpdate")
        else LOGGER.debug("starting harvest with parameters $params, force update: $forceUpdate")

        launch {
            activitySemaphore.withPermit {
                try {
                    harvestAdminAdapter.getDataSources(params)
                        .filter { it.dataType == "informationmodel" }
                        .filter { it.url != null }
                        .map {
                            async {
                                harvester.harvestInformationModelCatalog(
                                    it,
                                    Calendar.getInstance(),
                                    forceUpdate
                                )
                            }
                        }
                        .awaitAll()
                        .filterNotNull()
                        .also { updateService.updateUnionModel() }
                        .also {
                            if (params.harvestAllModels()) LOGGER.debug("completed harvest with parameters $params, force update: $forceUpdate")
                            else LOGGER.debug("completed harvest of all catalogs, force update: $forceUpdate")
                        }
                        .run { publisher.send(this) }
                } catch (ex: Exception) {
                    LOGGER.error("harvest failure", ex)
                }
            }
        }
    }
}
