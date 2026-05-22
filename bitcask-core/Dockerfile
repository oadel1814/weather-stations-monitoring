# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Install buf CLI for gRPC protobuf code generation
RUN curl -sSL \
    "https://github.com/bufbuild/buf/releases/download/v1.30.0/buf-$(uname -s)-$(uname -m)" \
    -o /usr/local/bin/buf && \
    chmod +x /usr/local/bin/buf

# Copy project files first for dependency caching
COPY pom.xml .
COPY buf.gen.yaml .
RUN mvn dependency:go-offline

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

COPY --from=builder /app/target/benchmarks.jar /app/bitcask.jar

# Configurable data directory — no hardcoded user paths
ENV BITCASK_DB_DIR=/data/bitcask
RUN mkdir -p /data/bitcask

EXPOSE 50051

CMD ["java", "-jar", "/app/bitcask.jar"]
