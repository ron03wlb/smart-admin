plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot Autoconfigure
    api(libs.spring.boot.autoconfigure)

    // Lock4j - Redisson implementation (reuse existing Redisson)
    api(libs.lock4j.redisson.spring.boot.starter)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Logging
    implementation(libs.spring.boot.starter.log4j2)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
