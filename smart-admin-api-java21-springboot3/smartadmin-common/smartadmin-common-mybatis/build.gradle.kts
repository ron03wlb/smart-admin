plugins {
    `java-library`
}

description = "SmartAdmin Common MyBatis - MyBatis Plus configuration and pagination utilities"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Common modules
    api(project(":smartadmin-common:smartadmin-common-core")) // Domain objects (PageParam, PageResult, ResponseDTO, BusinessException), SmartBeanUtil

    // Spring Boot
    api("org.springframework.boot:spring-boot-autoconfigure")

    // MyBatis Plus
    api("com.baomidou:mybatis-plus-spring-boot3-starter")
    api("com.baomidou:mybatis-plus-jsqlparser")

    // Utilities (for Lists in SmartPageUtil)
    api("com.google.guava:guava")
    api("org.apache.commons:commons-lang3")

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
