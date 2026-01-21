plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - Operation Log Module"

dependencies {
    // Spring Boot
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.validation)
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.starter.aop)

    // SA Common Core
    api(project(":sa-common:core"))

    // SA Base Infrastructure
    api(project(":sa-base-core"))
    api(project(":sa-base-mybatis"))
    api(project(":sa-base-web"))
    api(project(":sa-base-datasource"))  // For SmartIpUtil

    // Utilities
    api(libs.guava)
    api(libs.commons.lang3)
    api(libs.hutool.all)

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
