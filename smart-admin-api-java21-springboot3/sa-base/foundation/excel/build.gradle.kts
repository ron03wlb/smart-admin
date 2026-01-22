plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Foundation Excel - Excel utilities (future phase)"

dependencies {
    // Domain module
    api(project(":sa-base:foundation:domain"))

    // Excel libraries
    compileOnly(libs.fastexcel)
    compileOnly(libs.poi)

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
