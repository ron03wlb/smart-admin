plugins {
    `java-library`
}

description = "SmartAdmin Common Captcha - Image captcha generation and validation utilities"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Cache module (for storing captcha codes)
    api(project(":smartadmin-common:smartadmin-common-cache"))

    // Spring Boot Autoconfigure
    api("org.springframework.boot:spring-boot-autoconfigure")

    // Hutool (for captcha generation and image processing)
    api("cn.hutool:hutool-all")

    // Vavr (for Option)
    api("io.vavr:vavr")

    // Apache Commons
    api("org.apache.commons:commons-lang3")

    // Swagger Annotations (for @Schema in Form/VO classes)
    compileOnly("io.swagger.core.v3:swagger-annotations-jakarta:2.2.20")

    // Jakarta Validation (for @NotBlank in Form classes)
    compileOnly("jakarta.validation:jakarta.validation-api")

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
