plugins {
    `java-library`
}

description = "SmartAdmin Common Validation - Validation utilities and custom validators"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Core domain objects
    api(project(":smartadmin-common:smartadmin-common-core"))

    // Jakarta Validation API
    api("jakarta.validation:jakarta.validation-api")
    api("org.hibernate.validator:hibernate-validator")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
