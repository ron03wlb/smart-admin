plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - Operation Log Module"

dependencies {
    // Spring Boot
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.validation)
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.starter.aop)

    // SA Common Core
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))
    
    // SA Foundation - Core (temporary, for backward compatibility bridges)
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))

    // SA Base Infrastructure
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))
    
    // SA Foundation - Core (temporary, for backward compatibility bridges)
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))
    api(project(":smartadmin-common:smartadmin-common-mybatis"))
    api(project(":smartadmin-common:smartadmin-common-web"))
    api(project(":smartadmin-common:smartadmin-common-datasource"))  // For SmartIpUtil

    // Utilities
    api(libs.guava)
    api(libs.commons.lang3)
    api(libs.hutool.all)

    // API Documentation
    compileOnly(libs.knife4j.openapi3.jakarta)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Test - JUnit BOM and platform launcher for version alignment
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.spring.boot.starter.test)
}
