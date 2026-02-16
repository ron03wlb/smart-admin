plugins {
    `java-library`
}

description = "SmartAdmin API iGaming - iGaming module API contract layer"

dependencies {
    // BOM dependency management
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Vavr - Functional programming (MANDATORY for API contracts)
    api("io.vavr:vavr")

    // Jakarta Validation API
    api("jakarta.validation:jakarta.validation-api")
    api("org.hibernate.validator:hibernate-validator")

    // Jackson (for JSON serialization/deserialization)
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Swagger/OpenAPI Annotations (for API documentation)
    compileOnly("io.swagger.core.v3:swagger-annotations-jakarta:2.2.20")

    // SpotBugs Annotations (for @SuppressFBWarnings)
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Spring Framework (for future Feign compatibility)
    compileOnly("org.springframework:spring-context")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
