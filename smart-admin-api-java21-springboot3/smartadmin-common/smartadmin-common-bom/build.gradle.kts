plugins {
    `java-platform`
}

description = "SmartAdmin BOM - Bill of Materials for dependency version management"

dependencies {
    constraints {
        // ===================================================================
        // Spring Boot BOM
        // ===================================================================
        api("org.springframework.boot:spring-boot-dependencies:3.5.4")

        // ===================================================================
        // Core Libraries
        // ===================================================================
        // Vavr - Functional programming (MANDATORY for Service layer)
        api("io.vavr:vavr:0.10.6")

        // Lombok - Code generation
        api("org.projectlombok:lombok:1.18.34")

        // ===================================================================
        // Database & ORM
        // ===================================================================
        // MyBatis Plus
        api("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.12")
        api("com.baomidou:mybatis-plus-extension:3.5.12")
        api("com.baomidou:mybatis-plus-annotation:3.5.12")

        // PostgreSQL Driver
        api("org.postgresql:postgresql:42.7.5")

        // HikariCP (managed by Spring Boot, but explicit for reference)
        api("com.zaxxer:HikariCP:5.1.0")

        // P6Spy - SQL monitoring
        api("p6spy:p6spy:3.9.1")

        // ===================================================================
        // Cache & Redis
        // ===================================================================
        // Redisson - Redis client with distributed lock support
        api("org.redisson:redisson-spring-boot-starter:3.50.0")
        api("org.redisson:redisson:3.50.0")

        // JetCache - Multi-level cache
        api("com.alicp.jetcache:jetcache-starter-redis:2.7.7")
        api("com.alicp.jetcache:jetcache-starter-redis-lettuce:2.7.7")
        api("com.alicp.jetcache:jetcache-anno:2.7.7")

        // ===================================================================
        // Authentication & Authorization
        // ===================================================================
        // Sa-Token - Lightweight authentication framework
        api("cn.dev33:sa-token-spring-boot3-starter:1.44.0")
        api("cn.dev33:sa-token-core:1.44.0")
        api("cn.dev33:sa-token-redis-jackson:1.44.0")

        // ===================================================================
        // API Documentation
        // ===================================================================
        // Knife4j (Swagger UI enhancement)
        api("com.github.xiaoymin:knife4j-openapi3-jakarta-spring-boot-starter:4.6.0")
        api("com.github.xiaoymin:knife4j-openapi3-ui:4.6.0")

        // ===================================================================
        // Office Document Processing
        // ===================================================================
        // EasyExcel
        api("com.alibaba:easyexcel:4.0.4")

        // Apache POI (managed by EasyExcel, but explicit for reference)
        api("org.apache.poi:poi:5.3.0")
        api("org.apache.poi:poi-ooxml:5.3.0")

        // ===================================================================
        // Workflow & Rules Engine
        // ===================================================================
        // LiteFlow - Lightweight rule engine
        api("com.yomahub:liteflow-spring-boot-starter:2.13.1")
        api("com.yomahub:liteflow-core:2.13.1")

        // ===================================================================
        // Job Scheduling
        // ===================================================================
        // Snail-Job (if used)
        // api("com.aizuda:snail-job-client:1.x.x")

        // ===================================================================
        // Utilities
        // ===================================================================
        // Apache Commons Lang3
        api("org.apache.commons:commons-lang3:3.17.0")

        // Apache Commons Collections4
        api("org.apache.commons:commons-collections4:4.5.0-M3")

        // Apache Commons IO
        api("commons-io:commons-io:2.18.0")

        // Apache Tika (MIME type detection)
        api("org.apache.tika:tika-core:3.1.0")

        // Guava
        api("com.google.guava:guava:33.4.0-jre")

        // Hutool (Chinese utility library)
        api("cn.hutool:hutool-all:5.8.37")

        // ===================================================================
        // JSON Processing
        // ===================================================================
        // Jackson (managed by Spring Boot)
        api("com.fasterxml.jackson.core:jackson-databind:2.18.2")
        api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")

        // Fastjson2
        api("com.alibaba.fastjson2:fastjson2:2.0.59")
        api("com.alibaba.fastjson2:fastjson2-extension-spring6:2.0.59")

        // ===================================================================
        // Validation
        // ===================================================================
        // Jakarta Validation (managed by Spring Boot)
        api("jakarta.validation:jakarta.validation-api:3.1.0")
        api("org.hibernate.validator:hibernate-validator:8.0.2.Final")

        // ===================================================================
        // Security
        // ===================================================================
        // Bouncy Castle (Crypto provider for SM2/SM3/SM4)
        api("org.bouncycastle:bcprov-jdk18on:1.80")
        api("org.bouncycastle:bcpkix-jdk18on:1.80")

        // ===================================================================
        // Message Queue (Optional)
        // ===================================================================
        // Kafka (if used)
        // api("org.springframework.kafka:spring-kafka:3.x.x")

        // ===================================================================
        // Testing
        // ===================================================================
        // JUnit 5 (managed by Spring Boot)
        api("org.junit.jupiter:junit-jupiter:5.11.4")

        // AssertJ
        api("org.assertj:assertj-core:3.27.3")

        // Mockito
        api("org.mockito:mockito-core:5.15.2")
        api("org.mockito:mockito-junit-jupiter:5.15.2")

        // Testcontainers
        api("org.testcontainers:testcontainers:1.20.4")
        api("org.testcontainers:junit-jupiter:1.20.4")
        api("org.testcontainers:postgresql:1.20.4")

        // ArchUnit - Architecture validation
        api("com.tngtech.archunit:archunit-junit5:1.3.0")

        // ===================================================================
        // Logging (Managed by Spring Boot Logback)
        // ===================================================================
        // SLF4J API (managed by Spring Boot)
        api("org.slf4j:slf4j-api:2.0.17")

        // Logback (managed by Spring Boot)
        api("ch.qos.logback:logback-classic:1.5.16")
        api("ch.qos.logback:logback-core:1.5.16")

        // ===================================================================
        // IP Geolocation
        // ===================================================================
        // ip2region
        api("org.lionsoul:ip2region:2.7.0")

        // ===================================================================
        // Captcha
        // ===================================================================
        // Easy-Captcha
        api("com.github.whvcse:easy-captcha:1.6.2")
    }
}
