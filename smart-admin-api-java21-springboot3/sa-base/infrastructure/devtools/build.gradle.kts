plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base DevTools - Development utilities and tools (placeholder)"

dependencies {
    // SA Common Core - foundational domain objects and utilities
    api(project(":sa-base:foundation:core"))

    // SA Base Core - sa-base specific domain objects and utilities
    // Note: SmartExcelUtil is in sa-base-core for broader accessibility
    api(project(":sa-base:foundation:core"))

    // Note: Code Generator has been moved to sa-base-support:codegenerator module
    // This module is kept as a placeholder for future development tools

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)
}
