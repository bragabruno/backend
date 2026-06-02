plugins {
    id("org.springframework.boot")
}

dependencies {
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.data.redis)
    implementation(libs.spring.boot.starter.kafka)

    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.boot.starter.test)
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
