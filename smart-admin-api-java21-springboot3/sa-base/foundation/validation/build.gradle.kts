plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Foundation Validation - Validation utilities"

dependencies {
    // Domain module (for BaseEnum)
    api(project(":sa-base:foundation:domain"))

    // Validation API
    api(libs.spring.boot.starter.validation)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
