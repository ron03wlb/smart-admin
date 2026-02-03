plugins {
    `java-library`
}

description = "SmartAdmin Common Cache - Multi-level cache utilities (JetCache + Caffeine)"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Spring Boot Autoconfigure
    api("org.springframework.boot:spring-boot-autoconfigure")

    // JetCache - Redis with Lettuce (recommended for Spring Boot 3)
    api("com.alicp.jetcache:jetcache-starter-redis-lettuce")

    // Vavr (for Option)
    api("io.vavr:vavr")

    // Caffeine - Local cache for two-level caching (version managed by Spring Boot BOM)
    api("com.github.ben-manes.caffeine:caffeine") {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // SpotBugs Annotations
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
