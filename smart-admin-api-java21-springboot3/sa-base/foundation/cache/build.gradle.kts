plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot Autoconfigure
    api(libs.spring.boot.autoconfigure)

    // JetCache - Redis with Lettuce (recommended for Spring Boot 3)
    api(libs.jetcache.starter.redis.lettuce)

    // Vavr (for Option)
    api("io.vavr:vavr:0.10.4")

    // Caffeine - Local cache for two-level caching
    api(libs.caffeine) {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
