plugins {
    `java-library`
}

description = "SmartAdmin Common Core - Core domain objects and utilities"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Vavr - Functional programming (MANDATORY for Service layer)
    api("io.vavr:vavr")

    // Spring Context (for @Component, @Configuration annotations)
    compileOnly("org.springframework:spring-context")

    // Spring Web (for HttpServletRequest, HttpHeaders, etc.)
    compileOnly("org.springframework:spring-web")

    // Spring Boot (for @ConfigurationProperties)
    compileOnly("org.springframework.boot:spring-boot")

    // FastExcel (for Excel utilities) - TODO: 將 SmartExcelUtil 移到 smartadmin-common-excel 模塊
    compileOnly("cn.idev.excel:fastexcel:1.2.0")

    // Apache POI (for Excel operations) - TODO: 將 SmartExcelUtil 移到 smartadmin-common-excel 模塊
    compileOnly("org.apache.poi:poi:5.2.5")
    compileOnly("org.apache.poi:poi-ooxml:5.2.5")

    // Jakarta Validation API
    api("jakarta.validation:jakarta.validation-api")
    api("org.hibernate.validator:hibernate-validator")

    // Swagger/OpenAPI Annotations (for @Schema)
    compileOnly("io.swagger.core.v3:swagger-annotations-jakarta:2.2.20")

    // SpotBugs Annotations (for @SuppressFBWarnings)
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")

    // Jackson (for JSON serialization)
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.core:jackson-annotations")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // SLF4J Logging
    api("org.slf4j:slf4j-api")

    // Apache Commons Lang3
    api("org.apache.commons:commons-lang3")

    // Apache Commons Collections4 (for CollectionUtils)
    api("org.apache.commons:commons-collections4")

    // Hutool
    api("cn.hutool:hutool-all")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing - JUnit BOM and platform launcher for version alignment
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testCompileOnly("org.projectlombok:lombok")
    testAnnotationProcessor("org.projectlombok:lombok")
    // Hibernate Validator requires Expression Language at runtime
    testRuntimeOnly("org.glassfish.expressly:expressly:5.0.0")
    // Spring Beans for BeanUtils (required by SmartBeanUtil)
    testImplementation("org.springframework:spring-beans")
}
