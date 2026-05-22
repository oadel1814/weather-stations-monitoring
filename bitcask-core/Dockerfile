# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Install buf CLI for gRPC protobuf code generation
RUN curl -sSL \
    "https://github.com/bufbuild/buf/releases/download/v1.30.0/buf-$(uname -s)-$(uname -m)" \
    -o /usr/local/bin/buf && \
    chmod +x /usr/local/bin/buf

# Copy all POM files first for dependency caching
COPY pom.xml .
COPY bitcask-engine/pom.xml bitcask-engine/
COPY bitcask-grpc-server/pom.xml bitcask-grpc-server/
COPY bitcask-grpc-server/buf.gen.yaml bitcask-grpc-server/
RUN mvn dependency:go-offline -pl bitcask-grpc-server -am

# Copy source and build both modules
COPY bitcask-engine/src bitcask-engine/src
COPY bitcask-grpc-server/src bitcask-grpc-server/src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/bitcask-grpc-server/target/bitcask-grpc-server.jar /app/bitcask-grpc-server.jar

# Configurable data directory
ENV BITCASK_DB_DIR=/data/bitcask
RUN mkdir -p /data/bitcask

EXPOSE 50051

CMD ["java", "-jar", "/app/bitcask-grpc-server.jar"]
