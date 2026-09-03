# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn --batch-mode --no-transfer-progress -DskipTests dependency:go-offline

COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean verify

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install --yes --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 jerseysee \
    && chown jerseysee:jerseysee /app

COPY --from=build --chown=jerseysee:jerseysee \
    /workspace/target/jersey-see-0.0.1-SNAPSHOT.jar /app/app.jar

USER jerseysee
ENV SPRING_PROFILES_ACTIVE=production
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl --fail --silent --show-error "http://127.0.0.1:${PORT:-8080}/actuator/health" || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
