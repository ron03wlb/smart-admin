plugins {
    `java-library`
}

description = "SmartAdmin Common Data Masking - Data desensitization and masking utilities"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Core domain objects
    api(project(":smartadmin-common:smartadmin-common-core"))

    // Spring Boot Autoconfigure
    api("org.springframework.boot:spring-boot-autoconfigure")

    // Jackson (for serialization annotations)
    api("com.fasterxml.jackson.core:jackson-databind")

    // Hutool (for DesensitizedUtil, StrUtil)
    api("cn.hutool:hutool-all")

    // Apache Commons
    api("org.apache.commons:commons-lang3")
    api("org.apache.commons:commons-collections4")

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
