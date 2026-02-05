plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - Security Protect Module"

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
    api(project(":smartadmin-common:smartadmin-common-json"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))

    // SA Common Infrastructure
    api(project(":smartadmin-common:smartadmin-common-web"))
    api(project(":smartadmin-common:smartadmin-common-mybatis"))

    // SA Common Security (foundation module)
    api(project(":smartadmin-common:smartadmin-common-security"))

    // SA Support - Config service dependency
    api(project(":smartadmin-support:smartadmin-support-config"))

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
