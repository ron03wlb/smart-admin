plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Business Module - Business"

dependencies {
    // API Contract Layer
    api(project(":smartadmin-api:smartadmin-api-business"))

    // Spring Boot
    api(libs.spring.boot.starter.web)
    api(libs.spring.boot.starter.validation)

    // MyBatis Plus
    api(libs.mybatis.plus.spring.boot.starter)

    // SA Common Core
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-validation"))
    api(project(":smartadmin-common:smartadmin-common-web"))
    api(project(":smartadmin-common:smartadmin-common-mybatis"))
    api(project(":smartadmin-common:smartadmin-common-swagger"))
    api(project(":smartadmin-common:smartadmin-common-redis"))
    api(project(":smartadmin-common:smartadmin-common-json"))
    api(project(":smartadmin-common:smartadmin-common-excel"))
    api(project(":smartadmin-common:smartadmin-common-mq"))

    // SA Support（Business 需要的 support 模塊）
    api(project(":smartadmin-support:smartadmin-support-file"))
    api(project(":smartadmin-support:smartadmin-support-dict"))
    api(project(":smartadmin-support:smartadmin-support-operatelog"))
    api(project(":smartadmin-support:smartadmin-support-datatracer"))

    // SA Common - Additional modules needed
    api(project(":smartadmin-common:smartadmin-common-cache"))

    // FastExcel library (needed for goods module Excel import/export)
    api("cn.idev.excel:fastexcel:1.2.0")
    api("org.apache.poi:poi:5.2.5")
    api("org.apache.poi:poi-ooxml:5.2.5")

    // API Documentation
    compileOnly(libs.knife4j.openapi3.jakarta)

    // ArchUnit for architecture testing
    testImplementation(libs.archunit.junit5)

    // JUnit Platform Launcher (required for Gradle to run JUnit 5 tests)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
}
