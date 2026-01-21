plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - Dictionary Module"

dependencies {
    // Spring Boot
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.validation)
    api(libs.spring.boot.autoconfigure)

    // SA Common Core
    api(project(":sa-common:core"))

    // SA Base Infrastructure
    api(project(":sa-base-core"))
    api(project(":sa-base-mybatis"))
    api(project(":sa-base-web"))

    // SA Common Cache - JetCache for caching
    api(project(":sa-common:cache"))

    // SA Common Repeat Submit
    api(project(":sa-common:repeat-submit"))

    // Jackson (for DictDataDeserializer)
    api(libs.jackson.databind)

    // Utilities
    api(libs.commons.collections4)

    // API Documentation
    compileOnly(libs.knife4j.openapi3.jakarta)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
