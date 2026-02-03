plugins {
    `java-library`
}

description = "SmartAdmin Common Cache - Cache abstraction and JetCache integration"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Core utilities
    api(project(":smartadmin-common:smartadmin-common-core"))

    // JetCache - Multi-level cache
    api("com.alicp.jetcache:jetcache-starter-redis")
    api("com.alicp.jetcache:jetcache-anno")

    // Spring Context
    compileOnly("org.springframework:spring-context")
    compileOnly("org.springframework.boot:spring-boot")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // SLF4J Logging
    api("org.slf4j:slf4j-api")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
