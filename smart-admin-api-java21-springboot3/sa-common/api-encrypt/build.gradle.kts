plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencies {
    // SA Common Core (for ResponseDTO, DataTypeEnum)
    api(project(":sa-common:core"))

    // Spring Boot Autoconfigure
    api(libs.spring.boot.autoconfigure)

    // Spring Boot Web (for @ControllerAdvice)
    api(libs.spring.boot.starter.web)

    // Hutool Crypto (for AES, SM4)
    api(libs.hutool.all)

    // BouncyCastle (for SM4 support)
    api(libs.bcprov.jdk18on)

    // Apache Commons IO
    api(libs.commons.io)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Logging API (implementation provided by application)
    api(libs.slf4j.api)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
