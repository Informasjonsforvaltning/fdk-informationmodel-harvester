package no.fdk.fdk_informationmodel_harvester.rdf

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property

class ModellDCATAPNO {
    companion object {
        private val m = ModelFactory.createDefaultModel()
        val uri = "https://data.norge.no/vocabulary/modelldcatno#"
        val InformationModel: Property = m.createProperty( "${uri}InformationModel")
        val model: Property = m.createProperty( "${uri}model")
    }
}
