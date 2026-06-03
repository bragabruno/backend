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

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
}


