plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Token - Sa-Token configuration"

dependencies {
    // Spring Boot
    api(libs.spring.boot.autoconfigure)

    // SA Foundation - Domain objects (ResponseDTO, ErrorCode, RequestUser)
    api(project(":sa-base:foundation:domain"))
    
    // SA Foundation - Core (temporary, for backward compatibility bridges)
    api(project(":sa-base:foundation:core"))

    // SA Common - Security Protect (for SecurityConfigProvider)
    api(project(":sa-base:foundation:security-protect"))

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
