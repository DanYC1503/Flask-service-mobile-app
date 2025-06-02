FROM eclipse-temurin:22-jdk-alpine
WORKDIR /app

COPY /target/image-service.jar app.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
