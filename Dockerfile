FROM eclipse-temurin:17-jre-alpine

ENV TZ=Europe/Oslo
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

VOLUME /tmp
COPY /target/fdk-informationmodel-harvester.jar app.jar

RUN sh -c 'touch /app.jar'
CMD java -jar -XX:+UseZGC $JAVA_OPTS app.jar
