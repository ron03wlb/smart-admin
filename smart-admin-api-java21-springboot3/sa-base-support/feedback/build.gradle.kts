plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - Feedback Module"

dependencies {
    // Spring Boot Autoconfigure for AutoConfiguration support
    api(libs.spring.boot.autoconfigure)

    // Spring Boot Web (for controller support)
    api(libs.spring.boot.starter.web)

    // Spring Boot Validation
    api(libs.spring.boot.starter.validation)

    // MyBatis-Plus (for database access)
    api(libs.mybatis.plus.spring.boot.starter)

    // SA Common Core - foundational domain objects and utilities
    api(project(":sa-common:core"))

    // SA Base Core - sa-base specific domain objects and utilities
    api(project(":sa-base-core"))

    // SA Base - required for SupportBaseController, SwaggerTagConst, JSON serializers
    api(project(":sa-base"))

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
