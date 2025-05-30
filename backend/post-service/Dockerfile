FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

COPY post-service.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
