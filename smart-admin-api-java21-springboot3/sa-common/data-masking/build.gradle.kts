plugins {
    `java-library`
    id("io.spring.dependency-management")
}

dependencies {
    // Spring Boot Autoconfigure
    api(libs.spring.boot.autoconfigure)

    // Jackson (for serialization annotations)
    api(libs.jackson.databind)

    // Hutool (for DesensitizedUtil, StrUtil)
    api(libs.hutool.all)

    // Apache Commons
    api(libs.commons.lang3)
    api(libs.commons.collections4)

    // Lombok
    api(libs.lombok)
    annotationProcessor(libs.lombok)

    // SpotBugs annotations
    api(libs.spotbugs.annotations)

    // Logging
    implementation(libs.spring.boot.starter.log4j2)

    // Test
    testImplementation(libs.spring.boot.starter.test)
}
