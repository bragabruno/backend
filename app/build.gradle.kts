plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.dependency.management)
}

// Crucial: Tell this specific submodule how to resolve Spring versions
dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    // Spring Boot Starters (Versions are now pulled automatically from the BOM)
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.kafka)
    runtimeOnly(libs.postgresql)

    // Internal Monorepo Projects
    implementation(project(":common"))
    implementation(project(":transactions"))
    implementation(project(":rules"))
    implementation(project(":fraud-engine"))
    implementation(project(":case-management"))
    implementation(project(":security"))

    testImplementation(libs.spring.boot.starter.test)
}
