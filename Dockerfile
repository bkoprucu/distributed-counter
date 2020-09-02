FROM adoptopenjdk:11-jre-hotspot

COPY target/distributed-counter-0.1.1-SNAPSHOT.jar /opt/app.jar

ENV HAZELCAST_JAVA_OPTS="--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

EXPOSE 8080/tcp

WORKDIR /opt

ENTRYPOINT exec java $JAVA_OPTS $HAZELCAST_JAVA_OPTS -jar app.jar
