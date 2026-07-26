# syntax=docker/dockerfile:1
# Shared multi-stage build for the Spring Boot services.
# Pass the module name via --build-arg SERVICE=<wardrobe-service|avatar-service|outfit-composer-service>.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY . .
ARG SERVICE
# A BuildKit cache mount gives real dependency reuse across builds. The previous
# `dependency:go-offline || true` layer could not resolve the sibling `common`
# module, so it always failed and silently swallowed genuine errors with it.
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -B -pl ${SERVICE} -am package -DskipTests

FROM eclipse-temurin:25-jre
# curl is needed for the container healthcheck below (~1MB).
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --uid 10001 --create-home --home-dir /home/dressd dressd \
    && mkdir -p /app /data/media \
    && chown -R 10001:10001 /app /data/media

WORKDIR /app
ARG SERVICE
COPY --from=build --chown=10001:10001 /workspace/${SERVICE}/target/*.jar app.jar

# Nothing here needs root, and the media volume inherits this ownership.
USER 10001

# SERVICE_PORT is per-service; compose passes the matching value.
ARG SERVICE_PORT=8081
ENV SERVICE_PORT=${SERVICE_PORT}
EXPOSE ${SERVICE_PORT}

HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
    CMD curl -fsS "http://localhost:${SERVICE_PORT}/actuator/health" || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
