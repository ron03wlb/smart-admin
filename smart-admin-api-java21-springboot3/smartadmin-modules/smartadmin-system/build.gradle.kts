plugins {
    `java-library`
    id("io.spring.dependency-management")
}

description = "SmartAdmin Business Module - System"

dependencies {
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
    api(project(":smartadmin-common:smartadmin-common-token"))
    api(project(":smartadmin-common:smartadmin-common-datasource"))

    // Reflections library for DataScope scanning
    api(libs.reflections)

    // SA Support（System 模塊需要的 support 模塊）
    api(project(":smartadmin-support:smartadmin-support-loginlog"))
    api(project(":smartadmin-support:smartadmin-support-operatelog"))
    api(project(":smartadmin-support:smartadmin-support-file"))
    api(project(":smartadmin-support:smartadmin-support-dict"))
    api(project(":smartadmin-support:smartadmin-support-config"))
    api(project(":smartadmin-support:smartadmin-support-mail"))
    api(project(":smartadmin-support:smartadmin-support-message"))
    api(project(":smartadmin-support:smartadmin-support-changelog"))
    api(project(":smartadmin-support:smartadmin-support-heartbeat"))
    api(project(":smartadmin-support:smartadmin-support-securityprotect"))

    // SA Common - Additional modules needed
    api(project(":smartadmin-common:smartadmin-common-captcha"))
    api(project(":smartadmin-common:smartadmin-common-cache"))
    api(project(":smartadmin-common:smartadmin-common-api-encrypt"))

    // Temporary: Security-protect module (not yet fully migrated)
    // TODO: Remove this after security-protect is fully migrated to smartadmin-common-security
    api(project(":sa-base:foundation:security-protect"))

    // API Documentation
    compileOnly(libs.knife4j.openapi3.jakarta)

    // ArchUnit for architecture testing
    testImplementation(libs.archunit.junit5)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
