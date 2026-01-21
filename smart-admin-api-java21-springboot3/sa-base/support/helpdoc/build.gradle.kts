plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - Help Document Module"

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
    api(project(":sa-base:foundation:core"))

    // SA Common Repeat Submit - for @RepeatSubmit annotation
    api(project(":sa-base:foundation:repeat-submit"))

    // SA Base Core - sa-base specific domain objects and utilities
    api(project(":sa-base:foundation:core"))

    // SA Base Infrastructure - specific modules instead of monolithic sa-base
    api(project(":sa-base:infrastructure:web"))        // For SupportBaseController, JSON serializers
    api(project(":sa-base:infrastructure:mybatis"))    // For MyBatis Plus support
    api(project(":sa-base:infrastructure:swagger"))    // For SwaggerTagConst

    // SA Base Support - for FileKey serializers
    api(project(":sa-base:support:file"))  // For FileKeyVoSerializer/Deserializer

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
