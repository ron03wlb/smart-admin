plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot Autoconfigure
    api(libs.spring.boot.autoconfigure)

    // Spring Kafka
    api(libs.spring.kafka)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Logging API (implementation provided by application)
    api(libs.slf4j.api)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
