plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin iGaming Integration - End-to-end player journey orchestration"

dependencies {
    // iGaming Modules (Core Business Logic)
    api(project(":smartadmin-igaming:smartadmin-igaming-player"))
    api(project(":smartadmin-igaming:smartadmin-igaming-wallet"))
    api(project(":smartadmin-igaming:smartadmin-igaming-game"))
    api(project(":smartadmin-igaming:smartadmin-igaming-activity"))
    api(project(":smartadmin-igaming:smartadmin-igaming-risk"))

    // API Contract Layer
    api(project(":smartadmin-api:smartadmin-api-igaming"))

    // iGaming Common
    api(project(":smartadmin-igaming:smartadmin-igaming-common"))

    // Spring Boot
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.validation)

    // MyBatis Plus
    api(libs.mybatis.plus.spring.boot.starter)

    // SA Common Core
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-validation"))
    api(project(":smartadmin-common:smartadmin-common-web"))
    api(project(":smartadmin-common:smartadmin-common-mybatis"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))
    api(project(":smartadmin-common:smartadmin-common-redis"))
    api(project(":smartadmin-common:smartadmin-common-json"))
    api(project(":smartadmin-common:smartadmin-common-cache"))

    // iGaming Infrastructure (Multi-Tenant, Encryption, Token, Kafka)
    api(project(":smartadmin-common:smartadmin-common-tenant"))
    api(project(":smartadmin-common:smartadmin-common-security"))
    api(project(":smartadmin-common:smartadmin-common-token"))
    api(project(":smartadmin-common:smartadmin-common-mq"))

    // SA Support
    api(project(":smartadmin-support:smartadmin-support-operatelog"))
    api(project(":smartadmin-support:smartadmin-support-datatracer"))

    // JWT (for game session token generation)
    api("com.auth0:java-jwt:4.4.0")

    // API Documentation
    compileOnly(libs.knife4j.openapi3.jakarta)

    // Testing
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.testcontainers.kafka)
    testImplementation("org.flywaydb:flyway-core")  // For Flyway migration tests
    testImplementation("org.flywaydb:flyway-database-postgresql")  // PostgreSQL support
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs
    api(libs.spotbugs.annotations)
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
}
