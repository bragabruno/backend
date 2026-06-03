plugins {
    `java-library`
}

dependencies {
    api(project(":common"))
    api(project(":rules"))
    api(project(":case-management"))

    implementation(libs.spring.boot.starter.kafka)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation("io.micrometer:micrometer-core")

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.starter.kafka)
}
