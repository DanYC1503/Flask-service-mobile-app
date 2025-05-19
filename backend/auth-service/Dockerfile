FROM eclipse-temurin:22-jdk
WORKDIR /app
COPY target/auth-service-0.0.1-SNAPSHOT.war app.war
ENTRYPOINT ["java", "-war", "app.war"]