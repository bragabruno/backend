// Flyway 10 split database support into separate modules; the Gradle plugin
// resolves them from the buildscript classpath, so Postgres support must be
// added here for the `flywayMigrate` task to handle jdbc:postgresql URLs.
buildscript {
    dependencies {
        classpath("org.flywaydb:flyway-database-postgresql:${libs.versions.flywayPlugin.get()}")
    }
}

plugins {
    id("org.springframework.boot")
    alias(libs.plugins.flyway)
}

// Standalone migration runner used by CI (`./gradlew :app:flywayMigrate`).
// Reads the same SPRING_DATASOURCE_* env the CI job exports, with local-dev
// defaults; migrations live alongside the app resources.
flyway {
    url = System.getenv("SPRING_DATASOURCE_URL") ?: "jdbc:postgresql://localhost:5432/fraud_db"
    user = System.getenv("SPRING_DATASOURCE_USERNAME") ?: "fraud_user"
    password = System.getenv("SPRING_DATASOURCE_PASSWORD") ?: "fraud_pass"
    locations = arrayOf("filesystem:${projectDir}/src/main/resources/db/migration")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":security"))
    implementation(project(":transactions"))
    implementation(project(":rules"))
    implementation(project(":case-management"))
    implementation(project(":fraud-engine"))

    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.kafka)

    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)

    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    implementation(libs.springdoc.openapi.starter.webmvc.ui)

    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)

    implementation(libs.logstash.logback.encoder)

    implementation(libs.micrometer.registry.prometheus)

    // Distributed tracing (FRAUD-120): OpenTelemetry bridge + OTLP exporter, BOM-managed versions.
    implementation(libs.micrometer.tracing.bridge.otel)
    implementation(libs.opentelemetry.exporter.otlp)

    runtimeOnly(libs.postgresql)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.junit.jupiter)
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun>().configureEach {
    val envFile = rootProject.file(".env")
    if (envFile.exists()) {
        envFile.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                val key = parts[0].trim()
                val value = parts[1].trim().removeSurrounding("\"").removeSurrounding("'")
                environment(key, value)
            }
        }
    }
}
