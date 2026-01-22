plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base DevTools - Development utilities and tools (placeholder)"

dependencies {
    // SA Foundation - Domain objects
    api(project(":sa-base:foundation:domain"))
    
    // SA Foundation - Core (temporary, for backward compatibility bridges)
    api(project(":sa-base:foundation:core"))

    // Note: Code Generator has been moved to sa-base-support:codegenerator module
    // This module is kept as a placeholder for future development tools

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)
}
