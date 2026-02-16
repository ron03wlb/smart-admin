plugins {
    `java-library`
}

description = "SmartAdmin Common Tenant - Multi-tenant infrastructure"

dependencies {
    // BOM
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Common modules
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-mybatis"))

    // Spring Web (for Filter, HttpServletRequest)
    compileOnly("org.springframework.boot:spring-boot-starter-web")

    // Jackson (for tenant-aware serializer)
    compileOnly("com.fasterxml.jackson.core:jackson-databind")
    compileOnly("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // SLF4J Logging
    api("org.slf4j:slf4j-api")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // SpotBugs Annotations
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")

    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
