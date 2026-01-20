plugins {
    `java-library`
    id("org.springframework.boot") apply false
}

description = "SmartAdmin Base Core - Core domain objects and utilities specific to sa-base"

dependencies {
    // SA Common Core - foundational domain objects and utilities
    api(project(":sa-common:core"))

    // Spring Boot
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.starter.validation)
    api(libs.spring.boot.starter.web)  // For SmartRequestUtil, SmartResponseUtil, SmartExcelUtil

    // JSON
    api(libs.jackson.databind)

    // Persistence (for SmartPageUtil)
    api(libs.mybatis.plus.spring.boot.starter)

    // IP utilities (for SmartIpUtil)
    api(libs.ip2region)

    // Swagger/OpenAPI (for @Schema annotations)
    compileOnly(libs.knife4j.openapi3.jakarta)

    // Utilities
    api(libs.guava)
    api(libs.commons.lang3)
    api(libs.commons.collections4)
    api(libs.hutool.all)

    // Excel (optional for SmartExcelUtil)
    compileOnly(libs.fastexcel)
    compileOnly(libs.poi)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)
}
