FROM openjdk:11-jre-slim

COPY service/target/service-1.0-SNAPSHOT.jar /root/service.jar

# Rest port
EXPOSE 8080

# Hazelcast port
EXPOSE 9500

ENTRYPOINT ["java", "-jar", "/root/service.jar"]
