plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot Autoconfigure
    api(libs.spring.boot.autoconfigure)

    // Cache service dependency
    api(project(":sa-common:cache"))

    // Hutool for captcha generation
    api(libs.hutool.all)

    // Validation
    api(libs.spring.boot.starter.validation)

    // API Documentation
    api(libs.knife4j.openapi3.jakarta)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Apache Commons Lang
    api(libs.commons.lang3)

    // Logging API (implementation provided by application)
    api(libs.slf4j.api)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
