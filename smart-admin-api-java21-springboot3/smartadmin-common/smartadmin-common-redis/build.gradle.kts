plugins {
    `java-library`
}

description = "SmartAdmin Common Redis - Redis and Redisson configuration"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Common modules
    api(project(":smartadmin-common:smartadmin-common-core")) // Domain objects (ResponseDTO, ErrorCode)

    // Spring Boot
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-starter-data-redis")

    // Redisson (distributed locking, pub/sub, etc.)
    api("org.redisson:redisson-spring-boot-starter") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-actuator")
        exclude(group = "org.redisson", module = "redisson-spring-data-32")
    }

    // Objenesis (for Redisson serialization)
    api("org.objenesis:objenesis")

    // Commons Pool2 (for Redis connection pooling)
    api("org.apache.commons:commons-pool2")

    // SLF4J Logging
    api("org.slf4j:slf4j-api")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // SpotBugs Annotations
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
