FROM eclipse-temurin:22-jdk-alpine
WORKDIR /app

COPY /target/post-service.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
