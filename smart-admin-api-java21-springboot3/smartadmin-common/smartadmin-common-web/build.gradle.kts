plugins {
    `java-library`
}

description = "SmartAdmin Common Web - Web configuration, JSON serialization, and exception handling"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Common modules (migrated from foundation)
    api(project(":smartadmin-common:smartadmin-common-core"))         // Domain objects (ResponseDTO, ErrorCode)
    api(project(":smartadmin-common:smartadmin-common-json"))         // JSON serialization
    api(project(":smartadmin-common:smartadmin-common-repeat-submit"))// RepeatSubmitException
    api(project(":smartadmin-common:smartadmin-common-swagger"))      // SwaggerTagConst

    // Spring Boot Web
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-autoconfigure")

    // Apache HttpClient 5 (for RestClient configuration)
    api("org.apache.httpcomponents.client5:httpclient5")

    // Sa-Token (for NotPermissionException)
    api("cn.dev33:sa-token-spring-boot3-starter")

    // Jackson
    api("com.fasterxml.jackson.core:jackson-databind")

    // Utilities
    api("org.apache.commons:commons-lang3")

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
