FROM adoptopenjdk:11-jre-hotspot as builder
WORKDIR app
ARG JAR_FILE=target/distributed-counter-0.1.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
RUN java -Djarmode=layertools -jar app.jar extract


###
FROM adoptopenjdk:11-jre-hotspot
USER nobody
# Rest port
EXPOSE 8080/tcp

ENV HAZELCAST_JAVA_OPTS="--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

COPY --from=builder app/dependencies/ ./
COPY --from=builder app/spring-boot-loader/ ./
COPY --from=builder app/snapshot-dependencies/ ./
COPY --from=builder app/application/ ./
ENTRYPOINT exec java $HAZELCAST_JAVA_OPTS $JAVA_OPTS "org.springframework.boot.loader.JarLauncher"
