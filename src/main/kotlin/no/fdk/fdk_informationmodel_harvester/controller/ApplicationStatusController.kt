package no.fdk.fdk_informationmodel_harvester.controller

import no.fdk.fdk_informationmodel_harvester.repository.CatalogRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@CrossOrigin
class ApplicationStatusController(private val catalogRepository: CatalogRepository) {

    @GetMapping("/ping")
    fun ping(): ResponseEntity<Void> =
        ResponseEntity.ok().build()

    @GetMapping("/ready")
    fun ready(): ResponseEntity<Void> =
        ResponseEntity.ok().build()

    @GetMapping("/count")
    fun count(): ResponseEntity<Long> {
        try {
            return ResponseEntity.ok(catalogRepository.count())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
    }

}