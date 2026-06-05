# syntax=docker/dockerfile:1

# Stage 1: Build
# Base pinned by digest (eclipse-temurin:21-jdk-alpine).
FROM eclipse-temurin:25-jdk-alpine@sha256:30d9f87d702c2c1c601ed0d31e0c88ea1ea474ee7676cda7b7a59e759181c4dd AS builder
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
FROM eclipse-temurin:25-jre-alpine@sha256:c707c0d18cb9e8556380719f80d96a7529d0746fbb42143893949b98ed2f8943
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
