package no.fdk.imcat.utils;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Vocabulary definition for the
 * <a href="http://data.norge.no/informationmodel"> DCAT-NO-INFO Recommendation</a>.
 */
public class DCATNOINFO {
    /**
     * The namespace of the DCAT-NO-INFO vocabulary as a string
     */
    public static final String uri = "http://data.norge.no/informationmodel/";
    /**
     * The RDF model that holds the DCAT-NO-INFO entities
     */
    private static final Model m = ModelFactory.createDefaultModel();
    /**
     * The namespace of the DCAT-NO-INFO vocabulary
     */
    public static final Resource NAMESPACE = m.createResource(uri);
    /* ##########################################################
     * Defines DCAT-NO-INFO Classes
       ########################################################## */
    public static final Resource informationModel = m.createResource(uri + "InformationModel");
    public static final Resource modelElement = m.createResource(uri + "ModelElement");
    /* ##########################################################
     * Defines DCAT-NO-INFO Properties
       ########################################################## */
    public static final Property containsModelElement = m.createProperty(uri + "containsModelElement");
    public static final Property codeListReference = m.createProperty(uri + "codeListReference");
    public static final Property containsCodename = m.createProperty(uri + "containsCodename");
    public static final Property description = m.createProperty( uri + "description");
    public static final Property name = m.createProperty( uri + "name");
    public static final Property type = m.createProperty( uri + "type");
    public static final Property version = m.createProperty(uri + "version");
    public static final Property hasProperty = m.createProperty(uri + "hasProperty");
    public static final Property isSubclassOf = m.createProperty(uri + "isSubclassOf");
    public static final Property propertyType = m.createProperty(uri + "propertyType");
    public static final Property isDescribedBy = m.createProperty(uri + "isDescribedBy");
    public static final Property typeDefinitionReference = m.createProperty(uri + "typeDefinitionReference");
    public static final Property modelElementType = m.createProperty(uri + "modelElementType");
    public static final Property belongsToModule = m.createProperty(uri + "belongsToModule");
    /**
     * Returns the namespace of the DCAT-NO-INFO schema as a string
     *
     * @return the namespace of the DCAT-NO-INFO schema
     */
    public static String getURI() {
        return uri;
    }

}
