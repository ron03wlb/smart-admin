plugins {
    `java-library`
}

description = "SmartAdmin Common DataSource - DataSource configuration and IP utilities"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Common modules
    api(project(":smartadmin-common:smartadmin-common-core"))            // Domain objects
    api(project(":smartadmin-common:smartadmin-common-ip-geolocation"))  // Ip2RegionUtil
    api(project(":smartadmin-common:smartadmin-common-mybatis"))         // MybatisPlusFillHandler

    // Spring Boot
    api("org.springframework.boot:spring-boot-autoconfigure")

    // Database - PostgreSQL + P6Spy (SQL logging)
    api("org.postgresql:postgresql")
    api("p6spy:p6spy")

    // Apache Commons IO (for FileUtils in Ip2RegionListener)
    api("commons-io:commons-io")

    // SLF4J Logging
    api("org.slf4j:slf4j-api")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // SpotBugs Annotations
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
