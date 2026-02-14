plugins {
    `java-library`
}

description = "SmartAdmin Common Redis Lock - Distributed lock support with Redisson"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Core utilities
    api(project(":smartadmin-common:smartadmin-common-core"))

    // Lock4j - Redisson implementation (reuse existing Redisson)
    api("com.baomidou:lock4j-redisson-spring-boot-starter:2.2.7")

    // Redisson - Redis client with distributed lock support
    api("org.redisson:redisson-spring-boot-starter") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-actuator")
        exclude(group = "org.redisson", module = "redisson-spring-data-32")
    }
    api("org.redisson:redisson")

    // Spring Context
    compileOnly("org.springframework:spring-context")
    compileOnly("org.springframework.boot:spring-boot")

    // SpotBugs Annotations (for @SuppressFBWarnings)
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // SLF4J Logging
    api("org.slf4j:slf4j-api")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
