plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - Job Scheduling Module"

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

    // SA Common Repeat Submit
    api(project(":sa-common:repeat-submit"))

    // Quartz Scheduler (add to version catalog if not present)
    implementation("org.springframework.boot:spring-boot-starter-quartz")

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
