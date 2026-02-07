plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - File Management Module"

dependencies {
    // Spring Boot
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.validation)
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.starter.mail)

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

    // SA Common - Cache (for CacheService, CacheKeyConst)
    api(project(":smartadmin-common:smartadmin-common-cache"))

    // SA Common - Security Protect (for FileSecurityService, SecurityConfigProvider)
    api(project(":smartadmin-common:smartadmin-common-security"))

    // SA Common Repeat Submit
    api(project(":smartadmin-common:smartadmin-common-repeat-submit"))

    // Jackson (for FileKeySerializer/Deserializer)
    api(libs.jackson.databind)

    // File storage - AWS S3
    api(libs.aws.sdk.s3) {
        exclude(group = "commons-logging", module = "commons-logging")
    }

    // Document parsing
    api(libs.tika.core)

    // Utilities
    api(libs.hutool.all)
    api(libs.commons.io)
    api(libs.commons.lang3)

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
