plugins {
    `java-library`
}

description = "SmartAdmin Common IP Geolocation - IP address geolocation utilities"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // Core utilities
    api(project(":smartadmin-common:smartadmin-common-core"))

    // ip2region library
    api("org.lionsoul:ip2region")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // SLF4J Logging
    api("org.slf4j:slf4j-api")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
