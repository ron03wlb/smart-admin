plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin iGaming Activity - Activity engine and wagering tracking"

dependencies {
    // API Contract Layer
    api(project(":smartadmin-api:smartadmin-api-igaming"))
    // iGaming Common
    api(project(":smartadmin-igaming:smartadmin-igaming-common"))
    // iGaming cross-module dependencies (Activity is an aggregation module)
    api(project(":smartadmin-igaming:smartadmin-igaming-wallet"))
    api(project(":smartadmin-igaming:smartadmin-igaming-game"))
    api(project(":smartadmin-igaming:smartadmin-igaming-player"))
    // Job scheduling
    api(project(":smartadmin-support:smartadmin-support-job"))

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
    api(project(":smartadmin-common:smartadmin-common-cache"))

    // SA Support
    api(project(":smartadmin-support:smartadmin-support-operatelog"))
    api(project(":smartadmin-support:smartadmin-support-datatracer"))

    // API Documentation
    compileOnly(libs.knife4j.openapi3.jakarta)

    // Testing
    testImplementation(libs.archunit.junit5)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.spring.boot.starter.test)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs
    api(libs.spotbugs.annotations)
}

tasks.named<Test>("test") {
    useJUnitPlatform {
        excludeTags("integration")
    }
}
