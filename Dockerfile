# syntax=docker/dockerfile:1
FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd --system --uid 10001 jerseysee \
    && mkdir -p /tmp/jerseysee-uploads \
    && chown -R jerseysee:jerseysee /app /tmp/jerseysee-uploads

COPY --from=build /workspace/target/jersey-see-0.0.1-SNAPSHOT.jar /app/app.jar

USER jerseysee
EXPOSE 10000
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
