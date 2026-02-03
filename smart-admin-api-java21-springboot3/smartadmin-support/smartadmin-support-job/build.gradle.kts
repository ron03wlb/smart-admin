plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base Support - Job Scheduling Module"

dependencies {
    // Spring Boot
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.validation)
    api(libs.spring.boot.autoconfigure)

    // SA Common Core
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))
    
    // SA Foundation - Core (temporary, for backward compatibility bridges)
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))

    // SA Foundation - IP Geolocation (for IpGeolocationUtil)
    api(project(":smartadmin-common:smartadmin-common-ip-geolocation"))

    // SA Base Infrastructure
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))
    
    // SA Foundation - Core (temporary, for backward compatibility bridges)
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))
    api(project(":smartadmin-common:smartadmin-common-mybatis"))
    api(project(":smartadmin-common:smartadmin-common-web"))

    // SA Common Repeat Submit
    api(project(":smartadmin-common:smartadmin-common-repeat-submit"))

    // SA Common Redis Lock - for LockService
    api(project(":smartadmin-common:smartadmin-common-redis-lock"))

    // SA Base Redis - for RedissonClient
    api(project(":smartadmin-common:smartadmin-common-redis"))

    // SA Base Support Config - for ConfigDao and ConfigEntity (optional, for samples)
    compileOnly(project(":smartadmin-support:smartadmin-support-config"))

    // Quartz Scheduler (add to version catalog if not present)
    implementation("org.springframework.boot:spring-boot-starter-quartz")

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

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
