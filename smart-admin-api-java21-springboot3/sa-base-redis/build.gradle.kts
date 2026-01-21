plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Redis - Redis and Redisson configuration"

dependencies {
    // Spring Boot
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.starter.data.redis)

    // SA Base Core
    api(project(":sa-base-core"))

    // Redisson
    api(libs.redisson.spring.boot.starter) {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-actuator")
        exclude(group = "org.redisson", module = "redisson-spring-data-32")
    }
    api(libs.objenesis)
    api(libs.commons.pool2)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
