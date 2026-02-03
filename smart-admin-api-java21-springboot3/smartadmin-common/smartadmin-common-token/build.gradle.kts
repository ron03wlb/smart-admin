plugins {
    `java-library`
}

description = "SmartAdmin Common Token - Sa-Token configuration and authentication"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Common modules
    api(project(":smartadmin-common:smartadmin-common-core"))     // Domain objects (ResponseDTO, ErrorCode, RequestUser)
    api(project(":smartadmin-common:smartadmin-common-security")) // SecurityConfigProvider

    // Spring Boot
    api("org.springframework.boot:spring-boot-autoconfigure")

    // Sa-Token (authentication and authorization)
    api("cn.dev33:sa-token-spring-boot3-starter")
    api("cn.dev33:sa-token-redis-jackson")

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
