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

COPY --from=builder /workspace/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
