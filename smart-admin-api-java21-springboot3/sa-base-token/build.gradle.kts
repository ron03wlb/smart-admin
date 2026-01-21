plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Token - Sa-Token configuration"

dependencies {
    // Spring Boot
    api(libs.spring.boot.autoconfigure)

    // SA Base Core
    api(project(":sa-base-core"))

    // SA Common - Security Protect (for SecurityConfigProvider)
    api(project(":sa-common:security-protect"))

    // Sa-Token
    api(libs.sa.token.spring.boot.starter)
    api(libs.sa.token.redis.jackson)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
