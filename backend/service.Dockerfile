# Shared multi-stage build for the Spring Boot services.
# Pass the module name via --build-arg SERVICE=<wardrobe-service|avatar-service|outfit-composer-service>.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY common/pom.xml common/pom.xml
COPY wardrobe-service/pom.xml wardrobe-service/pom.xml
COPY avatar-service/pom.xml avatar-service/pom.xml
COPY outfit-composer-service/pom.xml outfit-composer-service/pom.xml
# Warm the dependency cache.
RUN mvn -q -B dependency:go-offline || true
COPY . .
ARG SERVICE
RUN mvn -q -B -pl ${SERVICE} -am package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app
ARG SERVICE
COPY --from=build /workspace/${SERVICE}/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
