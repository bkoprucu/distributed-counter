FROM openjdk:8-jre-slim

COPY target/distributed-counter-0.0.1-SNAPSHOT.jar /opt/app.jar

EXPOSE 8080/tcp

WORKDIR /opt

ENTRYPOINT exec java $JAVA_OPTS -jar app.jar
