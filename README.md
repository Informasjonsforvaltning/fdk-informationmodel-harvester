# fdk-informationmodel-harvester
fdk-informationmodel-harvester will harvest catalogs of information models according to the DCAT-AP-NO v2 specification.

## Requirements
- maven
- java 8
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

To inspect the Fuseki triple store, open your browser at http://localhost:3030/fuseki/
