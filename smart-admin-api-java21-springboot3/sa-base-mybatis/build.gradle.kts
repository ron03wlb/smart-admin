plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base MyBatis - MyBatis Plus configuration"

dependencies {
    // Spring Boot
    api(libs.spring.boot.autoconfigure)

    // SA Base Core
    api(project(":sa-base-core"))

    // MyBatis Plus
    api(libs.mybatis.plus.spring.boot.starter)
    api(libs.mybatis.plus.jsqlparser)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
