plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Base MyBatis - MyBatis Plus configuration"

dependencies {
    // Spring Boot
    api(libs.spring.boot.autoconfigure)

    // SA Foundation - Domain objects (PageParam, PageResult, ResponseDTO, BusinessException)
    api(project(":sa-base:foundation:domain"))

    // SA Foundation - Core utilities (SmartBeanUtil)
    api(project(":sa-base:foundation:core"))

    // Utilities (for Lists, StringUtils in SmartPageUtil)
    api(libs.guava)
    api(libs.commons.lang3)

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
