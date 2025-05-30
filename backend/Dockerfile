FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

COPY backend-springboot-service.jar app.jar

EXPOSE 5050
ENTRYPOINT ["java", "-jar", "app.jar"]
