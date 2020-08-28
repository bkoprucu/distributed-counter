FROM openjdk:11-jre-slim

COPY distributedcounter-service/target/distributedcounter-service-1.0-SNAPSHOT.jar /opt/app.jar

# Rest port
EXPOSE 8080/tcp

#ENTRYPOINT ["/bin/bash", "-c", "java --add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED -jar /root/distributedcounter-service.jar"]

WORKDIR /opt

# Params asked by Hazelcast
ENV HZ_JAVA_OPTS="--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"

ENTRYPOINT exec java $JAVA_OPTS $HZ_JAVA_OPTS -jar app.jar
