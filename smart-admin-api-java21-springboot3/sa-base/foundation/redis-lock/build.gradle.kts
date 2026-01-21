plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot Autoconfigure
    api(libs.spring.boot.autoconfigure)

    // Lock4j - Redisson implementation (reuse existing Redisson)
    api(libs.lock4j.redisson.spring.boot.starter)

    // Redisson - for RedissonService
    api(libs.redisson.spring.boot.starter) {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-actuator")
        exclude(group = "org.redisson", module = "redisson-spring-data-32")
    }

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
