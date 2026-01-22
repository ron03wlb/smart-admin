plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Foundation JSON - JSON serialization utilities"

dependencies {
    // Domain module (for BaseEnum, StringConst)
    api(project(":sa-base:foundation:domain"))

    // Validation module (for SmartEnumUtil)
    api(project(":sa-base:foundation:validation"))

    // Core module (for SmartStringUtil - temporary)
    api(project(":sa-base:foundation:core"))

    // Jackson
    api(libs.jackson.databind)

    // Commons Lang (for StringUtils)
    api(libs.commons.lang3)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Logging
    api(libs.slf4j.api)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
