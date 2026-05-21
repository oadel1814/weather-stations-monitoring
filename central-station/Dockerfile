FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
COPY bitcask-core/ ./bitcask-core/
COPY central-station/ ./central-station/

RUN mvn clean install -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/central-station/target/central-station-*-jar-with-dependencies.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]