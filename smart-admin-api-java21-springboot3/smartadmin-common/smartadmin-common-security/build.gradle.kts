plugins {
    `java-library`
}

description = "SmartAdmin Common Security - Security protection utilities (Level-3 security standards)"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Core domain objects
    api(project(":smartadmin-common:smartadmin-common-core"))

    // Spring Boot Autoconfigure
    api("org.springframework.boot:spring-boot-autoconfigure")

    // Validation
    api("org.springframework.boot:spring-boot-starter-validation")

    // Vavr (for Option)
    api("io.vavr:vavr")

    // Spring Web (for MultipartFile) - compileOnly as consumer provides it
    compileOnly("org.springframework.boot:spring-boot-starter-web")

    // Spring Security Crypto (for Argon2 password encoding)
    api("org.springframework.security:spring-security-crypto")

    // Apache Tika (for MIME type detection)
    api("org.apache.tika:tika-core")

    // Apache Commons
    api("org.apache.commons:commons-lang3")

    // Swagger/OpenAPI Annotations (for @Schema)
    compileOnly("io.swagger.core.v3:swagger-annotations-jakarta:2.2.20")

    // SLF4J Logging
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
