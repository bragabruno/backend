# syntax=docker/dockerfile:1

# Stage 1: Build
# Base pinned by digest (eclipse-temurin:21-jdk-alpine).
FROM eclipse-temurin:21-jdk-alpine@sha256:4fb80de7aeb277ad949cfbe89b4f504e50bb34c57fd908c5825236473d71e986 AS builder
WORKDIR /workspace

COPY gradlew gradlew.bat ./
COPY gradle gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY gradle/libs.versions.toml gradle/libs.versions.toml
COPY app/build.gradle.kts app/build.gradle.kts

RUN ./gradlew --no-daemon --warning-mode=all dependencies || true

COPY . .
RUN ./gradlew --no-daemon --warning-mode=all :app:bootJar

# Stage 2: Runtime
# Base pinned by digest (eclipse-temurin:21-jre-alpine).
FROM eclipse-temurin:21-jre-alpine@sha256:704db3c40204a44f471191446ddd9cda5d60dab40f0e15c6507b815ed897238b
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /workspace/app/build/libs/*.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Honour cgroup memory limits instead of assuming host RAM.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
