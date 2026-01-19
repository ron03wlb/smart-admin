plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot Autoconfigure
    api(libs.spring.boot.autoconfigure)

    // JetCache - Redis with Lettuce (recommended for Spring Boot 3)
    api(libs.jetcache.starter.redis.lettuce)

    // Caffeine - Local cache for two-level caching
    api(libs.caffeine) {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }

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
