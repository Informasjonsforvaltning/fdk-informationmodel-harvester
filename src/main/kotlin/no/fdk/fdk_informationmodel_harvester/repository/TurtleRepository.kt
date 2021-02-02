package no.fdk.fdk_informationmodel_harvester.repository

import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.stereotype.Service

@Service
class TurtleRepository(private val gridFsTemplate: GridFsTemplate) {
    fun saveFile(content: String, filename: String) {
        gridFsTemplate.store(content.byteInputStream(), filename)
    }

    fun readFileContent(filename: String): String? =
        gridFsTemplate.findOne(Query(Criteria.where("filename").`is`(filename)))
            ?.let { gridFsTemplate.getResource(it) }
            ?.content
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
}
