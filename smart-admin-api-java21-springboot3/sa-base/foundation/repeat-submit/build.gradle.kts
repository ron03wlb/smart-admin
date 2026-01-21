plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencies {
    // Foundation Core (for SmartRequestUtil, StringConst)
    api(project(":sa-base:foundation:core"))

    // Spring Boot Autoconfigure
    api(libs.spring.boot.autoconfigure)

    // Spring Boot AOP (for @Aspect)
    api(libs.spring.boot.starter.aop)

    // Spring Boot Web (for HttpServletRequest)
    api(libs.spring.boot.starter.web)

    // Guava (for Interner, ConcurrentMap)
    api(libs.guava)

    // Apache Commons Lang3 (for StringUtils)
    api(libs.commons.lang3)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Lock4j - Optional dependency for Redis implementation
    compileOnly(project(":sa-base:foundation:redis-lock"))

    // Logging
    implementation(libs.spring.boot.starter.log4j2)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
