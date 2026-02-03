plugins {
    `java-library`
}

description = "SmartAdmin Common MQ - Message queue utilities (Kafka integration)"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Spring Boot Autoconfigure
    api("org.springframework.boot:spring-boot-autoconfigure")

    // Spring Kafka (version managed by Spring Boot BOM)
    api("org.springframework.kafka:spring-kafka")

    // Vavr (for Option)
    api("io.vavr:vavr")

    // SLF4J Logging
    api("org.slf4j:slf4j-api")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // SpotBugs Annotations
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.6")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.awaitility:awaitility:4.2.0")
    testAnnotationProcessor("org.projectlombok:lombok")
}
