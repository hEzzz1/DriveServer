# syntax=docker/dockerfile:1.7

FROM maven:3.9.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B dependency:go-offline

COPY src/ src/

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:17-jre

WORKDIR /app

ENV TZ=Asia/Shanghai

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /workspace/target/*.jar /app/app.jar

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=5s --retries=12 --start-period=30s \
    CMD curl --fail --silent --show-error "http://127.0.0.1:${SERVER_PORT:-8080}/actuator/health" >/dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
