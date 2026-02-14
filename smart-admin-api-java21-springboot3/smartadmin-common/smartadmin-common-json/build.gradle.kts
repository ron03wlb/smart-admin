plugins {
    `java-library`
}

description = "SmartAdmin Common JSON - JSON serialization and deserialization utilities"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Core domain objects
    api(project(":smartadmin-common:smartadmin-common-core"))

    // Validation utilities (for SmartEnumUtil)
    api(project(":smartadmin-common:smartadmin-common-validation"))

    // Spring Context (for @Component annotation)
    compileOnly("org.springframework:spring-context")

    // Jakarta Annotations (for @PostConstruct)
    compileOnly("jakarta.annotation:jakarta.annotation-api")

    // Jackson (for JSON processing)
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("com.fasterxml.jackson.core:jackson-core")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Hutool (for JSON utilities)
    api("cn.hutool:hutool-all")

    // Apache Commons
    api("org.apache.commons:commons-lang3")

    // SLF4J Logging
    api("org.slf4j:slf4j-api")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // SpotBugs Annotations
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
