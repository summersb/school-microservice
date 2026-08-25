# Build context is the repository root (see docker-compose.yml), because this
# is a Maven multi-module reactor build and each service's module needs the
# parent pom to build.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
COPY student-service/pom.xml student-service/pom.xml
COPY faculty-service/pom.xml faculty-service/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY classes-service/pom.xml classes-service/pom.xml
COPY schedule-service/pom.xml schedule-service/pom.xml
COPY register-service/pom.xml register-service/pom.xml
COPY integration-tests/pom.xml integration-tests/pom.xml
# Shared Flyway migrations -- only used by this module's Dev Services database
# in tests (see application.properties); the prod stack's db-migrate job
# applies them directly instead.
COPY db/migration db/migration
COPY register-service/src register-service/src
RUN mvn -B -pl register-service -am -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/register-service/target/quarkus-app/lib/ /app/lib/
COPY --from=build /build/register-service/target/quarkus-app/*.jar /app/
COPY --from=build /build/register-service/target/quarkus-app/app/ /app/app/
COPY --from=build /build/register-service/target/quarkus-app/quarkus/ /app/quarkus/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
