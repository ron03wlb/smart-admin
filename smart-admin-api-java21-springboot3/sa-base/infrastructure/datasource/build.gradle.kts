plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base DataSource - DataSource configuration and IP utilities"

dependencies {
    // Spring Boot
    api(libs.spring.boot.autoconfigure)

    // SA Foundation - Domain objects
    api(project(":sa-base:foundation:domain"))
    
    // SA Foundation - Core (temporary, for backward compatibility bridges)
    api(project(":sa-base:foundation:core"))

    // SA Foundation - IP Geolocation
    api(project(":sa-base:foundation:ip-geolocation"))

    // SA Base MyBatis (for MybatisPlusFillHandler)
    api(project(":sa-base:infrastructure:mybatis"))

    // Database - PostgreSQL + HikariCP (Spring Boot default)
    api(libs.postgresql)
    api(libs.p6spy)

    // Apache Commons IO (for FileUtils in Ip2RegionListener)
    api(libs.commons.io)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
