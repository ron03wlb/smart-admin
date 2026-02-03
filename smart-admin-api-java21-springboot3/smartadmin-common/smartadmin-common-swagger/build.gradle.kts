plugins {
    `java-library`
}

description = "SmartAdmin Common Swagger - Swagger/Knife4j configuration and customizers"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Common modules (migrated from foundation)
    api(project(":smartadmin-common:smartadmin-common-core"))       // Domain objects (ResponseDTO, ErrorCode)
    api(project(":smartadmin-common:smartadmin-common-validation")) // @CheckEnum annotation
    api(project(":smartadmin-common:smartadmin-common-api-encrypt"))// ApiDecrypt, ApiEncrypt annotations

    // Spring Boot
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-starter-web")

    // API Documentation - Springdoc OpenAPI (required for customizers and models)
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")

    // Guava (for Lists utility)
    api("com.google.guava:guava")

    // Sa-Token (for @SaCheckPermission, @SaCheckRole, @SaMode annotations)
    api("cn.dev33:sa-token-spring-boot3-starter")

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
