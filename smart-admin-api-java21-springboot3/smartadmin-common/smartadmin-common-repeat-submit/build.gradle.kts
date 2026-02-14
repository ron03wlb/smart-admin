plugins {
    `java-library`
}

description = "SmartAdmin Common Repeat Submit - Repeat submission prevention utilities"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Core domain objects
    api(project(":smartadmin-common:smartadmin-common-core"))

    // Redis Lock (optional for Redis implementation)
    compileOnly(project(":smartadmin-common:smartadmin-common-redis-lock"))

    // Spring Boot Autoconfigure
    api("org.springframework.boot:spring-boot-autoconfigure")

    // Spring Boot AOP (for @Aspect)
    api("org.springframework.boot:spring-boot-starter-aop")

    // Spring Boot Web (for HttpServletRequest)
    api("org.springframework.boot:spring-boot-starter-web")

    // Guava (for Interner, ConcurrentMap)
    api("com.google.guava:guava")

    // Apache Commons Lang3
    api("org.apache.commons:commons-lang3")

    // SLF4J Logging (not Log4j2, use SLF4J API)
    api("org.slf4j:slf4j-api")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // SpotBugs Annotations
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
