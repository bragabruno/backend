import org.gradle.api.artifacts.VersionCatalogsExtension as VersionCatalogExtensions
import net.ltgt.gradle.errorprone.errorprone

plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.dependency.management) apply false
    alias(libs.plugins.errorprone) apply false
    java
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "net.ltgt.errorprone")

    group = "com.bragdev"
    version = "0.0.1-SNAPSHOT"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    repositories {
        mavenCentral()
    }

    // Safely pull the libraries from the rootProject catalog container
    val catalog = rootProject.extensions.getByType<VersionCatalogExtensions>().named("libs")

    dependencies {
        compileOnly(catalog.findLibrary("lombok").get())
        annotationProcessor(catalog.findLibrary("lombok").get())
        annotationProcessor(catalog.findLibrary("nullaway").get())

        "errorprone"(catalog.findLibrary("errorprone-core").get())
        "errorprone"(catalog.findLibrary("nullaway").get())
    }

    tasks.withType<JavaCompile>().configureEach {
        // Add "-Xlint:-processing" to turn off warning noise for unclaimed annotations
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Werror"))

        plugins.withId("net.ltgt.errorprone") {
            val errorproneOptions = (options as ExtensionAware).extensions.getByName("errorprone")
                as net.ltgt.gradle.errorprone.ErrorProneOptions

            errorproneOptions.option("NullAway:AnnotatedPackages", "com.bragdev")
        }
    }
}
