FROM openjdk:8-jre-slim

COPY service/target/service-1.0-SNAPSHOT.jar /root/service.jar

# Rest port
EXPOSE 8080

# Hazelcast port
EXPOSE 9500

CMD ["java", "-jar", "/root/service.jar"]
