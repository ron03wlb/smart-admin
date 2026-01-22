plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Foundation IP Geolocation - IP address utilities"

dependencies {
    // Domain module (for StringConst)
    api(project(":sa-base:foundation:domain"))

    // Core module (for SmartStringUtil - temporary)
    api(project(":sa-base:foundation:core"))

    // IP2Region library
    api(libs.ip2region)

    // Commons Lang
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
