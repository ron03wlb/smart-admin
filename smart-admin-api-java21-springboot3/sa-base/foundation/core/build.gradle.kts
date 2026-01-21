plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencies {
    // ========== Original dependencies (from foundation:core) ==========
    // Spring Boot Autoconfigure
    api(libs.spring.boot.autoconfigure)

    // Validation API (for jakarta.validation annotations)
    api(libs.spring.boot.starter.validation)

    // Jackson for JSON processing
    api(libs.jackson.databind)

    // Swagger/OpenAPI annotations
    compileOnly(libs.knife4j.openapi3.jakarta)

    // Utilities
    api(libs.guava)
    api(libs.commons.lang3)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Logging API
    api(libs.slf4j.api)

    // Test
    testImplementation(libs.spring.boot.starter.test)

    // ========== Additional dependencies (from infrastructure:core) ==========
    // For SmartRequestUtil, SmartResponseUtil, SmartExcelUtil
    api(libs.spring.boot.starter.web)

    // For SmartPageUtil
    api(libs.mybatis.plus.spring.boot.starter)

    // For SmartIpUtil
    api(libs.ip2region)

    // Additional utilities
    api(libs.hutool.all)
    api(libs.commons.collections4)

    // Excel dependencies (optional)
    compileOnly(libs.fastexcel)
    compileOnly(libs.poi)
}
