# Multi-stage build for production (e.g. Alibaba Cloud ACR + ECS).
# Build: docker build -t live-more-api:latest .
# Run:  docker run --rm -p 8080:8080 --env-file .env live-more-api:latest

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build --chown=spring:spring /build/target/live-more-api-0.1.0-SNAPSHOT.jar app.jar
USER spring:spring
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
