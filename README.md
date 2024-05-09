# fdk-informationmodel-harvester

The harvest process is triggered by messages from RabbitMQ with the routing key `informationmodel.*.HarvestTrigger`, a message will call the method `initiateHarvest` in the class `HarvesterActivity`. The actual harvest will start when `activitySemaphore` has an available permit, when there are no available permits all messages will be queued by the semaphore.

The body of the trigger message has 3 relevant parameters:
- `dataSourceId` - Triggers the harvest of a specific source from fdk-harvest-admin
- `publisherId` - Triggers the harvest of all sources for the specified organization number.
- `forceUpdate` - Indicates that the harvest should be performed, even when no changes are detected in the source

A triggered harvest will download all relevant sources from fdk-harvest-admin, download everything from the source and try to read it as a RDF graph via a jena Model. If the source is successfully parsed as a jena Model it will be compared to the last harvest of the same source. The harvest process will continue if the source is not isomorphic to the last harvest or `forceUpdate` is true.

The actual harvest process will first find all catalogs, resources with the type `dcat:Catalog`, blank node catalogs will be ignored. And then find all information model each catalog contains, indicated by the predicate `modelldcatno:model` and type `modelldcatno:InformationModel`, blank node information models will be ignored.
When all catalogs and information models have been found a recursive function will create a graph with every contained triple for all catalogs and information model.

The process will save metadata for both information models and catalogs:
- `uri` - The IRI for the resource, is used as the database id
- `fdkId` - The UUID used for the resource used in the context of FDK, is a generated hash of the uri if nothing else is set.
- `isPartOf` - Only relevant for information models, is the uri of the catalog it belongs to.
- `removed` - Only relevant for information models, is set to true if the information model has been removed from the source.
- `issued` - The timestamp of the first time the resource was harvested
- `modified` - The timestamp of the last time a harvest of the resource found changes in the resource graph

All blank nodes will be [skolemized](https://www.w3.org/wiki/BnodeSkolemization) in the resource graphs.

When all sources from the trigger has been processed a new rabbit message will be published with the routing key `informationmodels.harvested`, the message body will be a list of harvest reports, one report for each source from fdk-harvest-admin.

When the rabbit message has been published the semaphore permit is released and a new harvest trigger can be processed.

## Requirements
- maven
- java 17
- docker
- docker-compose

## Run tests
```
mvn verify
```

## Run locally
```
docker-compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=develop
```

Then in another terminal e.g.
```
% curl http://localhost:8080/services
```

## Datastore
To inspect the MongoDB datastore, open a terminal and run:
```
docker-compose exec mongodb mongo
use admin
db.auth("admin","admin")
use informationModelHarvester
db.informationmodel.find()
db.catalog.find()
db.misc.find()
```
