plugins {
    `java-library`
}

description = "SmartAdmin Common API Encrypt - API request/response encryption utilities (AES, SM4)"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Core domain objects
    api(project(":smartadmin-common:smartadmin-common-core"))

    // Spring Boot Autoconfigure
    api("org.springframework.boot:spring-boot-autoconfigure")

    // Spring Boot Web (for @ControllerAdvice)
    api("org.springframework.boot:spring-boot-starter-web")

    // Hutool Crypto (for AES, SM4)
    api("cn.hutool:hutool-all")

    // BouncyCastle (for SM4 support)
    api("org.bouncycastle:bcprov-jdk18on")

    // Apache Commons IO
    api("commons-io:commons-io")

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
