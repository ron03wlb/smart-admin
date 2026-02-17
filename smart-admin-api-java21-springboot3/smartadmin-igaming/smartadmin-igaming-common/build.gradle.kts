plugins {
    `java-library`
}

description = "SmartAdmin iGaming Common - Shared enums, base entities, constants"

dependencies {
    // BOM dependency management
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // SA Common Core
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-mybatis"))

    // Multi-Tenant (TenantContext in common-core, config in common-tenant)
    api(project(":smartadmin-common:smartadmin-common-tenant"))

    // Vavr - Functional programming (MANDATORY)
    api("io.vavr:vavr")

    // Jackson (for JSON serialization/deserialization)
    api("com.fasterxml.jackson.core:jackson-annotations")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // SpotBugs Annotations
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")
}
