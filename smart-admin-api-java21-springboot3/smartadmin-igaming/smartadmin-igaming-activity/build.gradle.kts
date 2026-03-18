plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin iGaming Activity - Activity engine and wagering tracking"

dependencies {
    // API Contract Layer
    api(project(":smartadmin-api:smartadmin-api-igaming"))
    // iGaming Common
    api(project(":smartadmin-igaming:smartadmin-igaming-common"))
    // iGaming cross-module dependencies (Activity is an aggregation module)
    api(project(":smartadmin-igaming:smartadmin-igaming-wallet"))
    api(project(":smartadmin-igaming:smartadmin-igaming-game"))
    api(project(":smartadmin-igaming:smartadmin-igaming-player"))
    // Job scheduling
    api(project(":smartadmin-support:smartadmin-support-job"))
    // Kafka MQ (for event consuming)
    api(project(":smartadmin-common:smartadmin-common-mq"))

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

    // SA Support
    api(project(":smartadmin-support:smartadmin-support-operatelog"))
    api(project(":smartadmin-support:smartadmin-support-datatracer"))
    api(project(":smartadmin-support:smartadmin-support-liteflow"))

    // API Documentation
    compileOnly(libs.knife4j.openapi3.jakarta)

    // Testing
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.spring.boot.starter.test)

    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")

    // AssertJ for fluent assertions
    testImplementation("org.assertj:assertj-core:3.24.2")

    // Mockito for unit tests
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")

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
