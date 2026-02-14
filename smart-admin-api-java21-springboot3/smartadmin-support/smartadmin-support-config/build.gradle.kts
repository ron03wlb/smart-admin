plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - System Configuration Module"

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
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))
    
    // SA Foundation - Core (temporary, for backward compatibility bridges)
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))

    // SA Base Core - sa-base specific domain objects and utilities
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))
    
    // SA Foundation - Core (temporary, for backward compatibility bridges)
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))

    // SA Base Infrastructure
    api(project(":smartadmin-common:smartadmin-common-mybatis"))
    api(project(":smartadmin-common:smartadmin-common-web"))

    // SA Common Repeat Submit - for @RepeatSubmit annotation
    api(project(":smartadmin-common:smartadmin-common-repeat-submit"))

    // SA Base Support Reload - for @SmartReload annotation and ReloadConst
    api(project(":smartadmin-support:smartadmin-support-reload"))

    // Utilities
    api(libs.commons.lang3)

    // API Documentation
    compileOnly(libs.knife4j.openapi3.jakarta)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Test - JUnit BOM and platform launcher for version alignment
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.spring.boot.starter.test)
}
