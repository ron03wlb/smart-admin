plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - File Management Module"

dependencies {
    // Spring Boot
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.validation)
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.starter.mail)

    // SA Common Core
    api(project(":sa-common:core"))

    // SA Base Infrastructure
    api(project(":sa-base-core"))
    api(project(":sa-base-mybatis"))
    api(project(":sa-base-web"))

    // SA Common - Cache (for CacheService, CacheKeyConst)
    api(project(":sa-common:cache"))

    // SA Common - Security Protect (for FileSecurityService, SecurityConfigProvider)
    api(project(":sa-common:security-protect"))

    // SA Common Repeat Submit
    api(project(":sa-common:repeat-submit"))

    // Jackson (for FileKeySerializer/Deserializer)
    api(libs.jackson.databind)

    // File storage - AWS S3
    api(libs.aws.sdk.s3) {
        exclude(group = "commons-logging", module = "commons-logging")
    }

    // Document parsing
    api(libs.tika.core)

    // Utilities
    api(libs.hutool.all)
    api(libs.commons.io)
    api(libs.commons.lang3)

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
