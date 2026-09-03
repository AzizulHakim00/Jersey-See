# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress -DskipTests dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 10001 jerseysee \
    && chown jerseysee:jerseysee /app

COPY --from=build --chown=jerseysee:jerseysee \
    /workspace/target/jersey-see-0.0.1-SNAPSHOT.jar /app/app.jar

USER jerseysee
ENV SPRING_PROFILES_ACTIVE=production
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
