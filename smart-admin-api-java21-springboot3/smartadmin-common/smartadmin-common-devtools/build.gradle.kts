plugins {
    `java-library`
}

description = "SmartAdmin Common DevTools - Development utilities and tools (placeholder for future expansion)"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Common modules
    api(project(":smartadmin-common:smartadmin-common-core")) // Domain objects

    // Note: Code Generator has been moved to smartadmin-support:codegenerator module
    // This module is kept as a placeholder for future development tools

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}
