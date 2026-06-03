plugins {
    `java-library`
}

dependencies {
    api(libs.spring.boot.starter.webmvc)
    api(libs.spring.boot.starter.data.jpa)
    api(libs.spring.boot.starter.validation)

    api(libs.flyway.core)
    api(libs.flyway.database.postgresql)

    api(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)

    api(libs.jackson.databind)
    api(libs.jackson.datatype.jsr310)

    api("io.micrometer:micrometer-core")

    // Required by the transactional outbox relay and Kafka consumer error handler in this module.
    implementation(libs.spring.boot.starter.kafka)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}


