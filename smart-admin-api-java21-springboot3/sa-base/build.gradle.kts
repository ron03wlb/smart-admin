plugins {
    `java-library`
    id("io.spring.dependency-management")
}

// Get environment from project property (defaults to 'dev')
val activeEnv: String by lazy {
    (project.findProperty("env") as? String) ?: "dev"
}

configurations {
    // Exclude Tomcat globally - using Undertow instead
    all {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
        // Exclude conflicting logging frameworks - using Log4j2
        exclude(group = "ch.qos.logback", module = "logback-classic")
        exclude(group = "ch.qos.logback", module = "logback-core")
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    }
    testImplementation {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
    testRuntimeOnly {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
}

dependencies {
    // Spring Boot Starters - use api to expose to dependent modules
    api(libs.spring.boot.starter.aop) {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }

    api(libs.spring.boot.starter.data.redis) {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter")
    }

    api(libs.spring.boot.starter.web) {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-tomcat")
    }

    // Undertow - lightweight, high-performance embedded server
    api(libs.spring.boot.starter.undertow)

    api(libs.spring.boot.starter.log4j2)
    api(libs.spring.boot.starter.validation)
    api(libs.spring.boot.starter.mail)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)

    // Spring Security
    api(libs.spring.security.crypto)

    // Sa-Token
    api(libs.sa.token.spring.boot.starter)
    api(libs.sa.token.redis.jackson)

    // Database - PostgreSQL + HikariCP (Spring Boot default)
    api(libs.postgresql)
    api(libs.p6spy)

    // MyBatis-Plus
    api(libs.mybatis.plus.spring.boot.starter)
    api(libs.mybatis.plus.jsqlparser)

    // Cache
    api(libs.caffeine) {
        exclude(group = "com.google.errorprone", module = "error_prone_annotations")
    }

    // Redis
    api(libs.redisson.spring.boot.starter) {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-actuator")
        exclude(group = "org.redisson", module = "redisson-spring-data-32")
    }
    api(libs.objenesis)
    api(libs.commons.pool2)

    // ========== Layer 0: Foundation (9 modules, formerly sa-common) ==========
    api(project(":sa-base:foundation:core"))
    api(project(":sa-base:foundation:mq"))
    api(project(":sa-base:foundation:cache"))
    api(project(":sa-base:foundation:redis-lock"))
    api(project(":sa-base:foundation:api-encrypt"))
    api(project(":sa-base:foundation:captcha"))
    api(project(":sa-base:foundation:repeat-submit"))
    api(project(":sa-base:foundation:data-masking"))
    api(project(":sa-base:foundation:security-protect"))

    // ========== Layer 1: Infrastructure (7 modules, formerly sa-base-*) ==========
    // Note: infrastructure:core merged into foundation:core to resolve Gradle circular dependency
    api(project(":sa-base:infrastructure:web"))
    api(project(":sa-base:infrastructure:mybatis"))
    api(project(":sa-base:infrastructure:redis"))
    api(project(":sa-base:infrastructure:token"))
    api(project(":sa-base:infrastructure:datasource"))
    api(project(":sa-base:infrastructure:swagger"))
    api(project(":sa-base:infrastructure:devtools"))

    // ========== Layer 2: Business Support (17 modules, formerly sa-base-support) ==========
    api(project(":sa-base:support:config"))         // System configuration management
    api(project(":sa-base:support:dict"))           // Data dictionary with caching
    api(project(":sa-base:support:file"))           // File upload (local/S3)
    api(project(":sa-base:support:job"))            // Scheduled task management
    api(project(":sa-base:support:datatracer"))     // Data change tracking
    api(project(":sa-base:support:loginlog"))       // Login log
    api(project(":sa-base:support:operatelog"))     // Operation log
    api(project(":sa-base:support:reload"))         // Dynamic configuration reload
    api(project(":sa-base:support:codegenerator"))  // Code generator
    api(project(":sa-base:support:table"))          // Table column customization
    api(project(":sa-base:support:feedback"))       // User feedback
    api(project(":sa-base:support:changelog"))      // Change log
    api(project(":sa-base:support:message"))        // Internal messaging
    api(project(":sa-base:support:heartbeat"))      // Heartbeat monitoring
    api(project(":sa-base:support:mail"))           // Email service
    api(project(":sa-base:support:serialnumber"))   // Serial number generation
    api(project(":sa-base:support:helpdoc"))        // Help documentation

    // API Documentation
    api(libs.knife4j.openapi3.jakarta)

    // HTTP Client
    api(libs.httpclient5)

    // Utilities
    api(libs.guava)
    api(libs.concurrentlinkedhashmap.lru)
    api(libs.reflections) {
        exclude(group = "com.google.guava", module = "guava")
    }
    api(libs.hutool.all)
    api(libs.commons.lang3)
    api(libs.commons.collections4)
    api(libs.commons.io)
    api(libs.commons.compress)
    api(libs.commons.codec)
    api(libs.commons.text)

    // Template Engines
    api(libs.velocity.engine.core)
    api(libs.velocity.tools.generic)
    api(libs.freemarker)

    // Security
    api(libs.ip2region)
    api(libs.bcprov.jdk18on)

    // Excel & Documents
    api(libs.fastexcel) {
        exclude(group = "ch.qos.logback", module = "logback-classic")
        exclude(group = "org.bouncycastle", module = "bcprov-jdk15on")
    }
    api(libs.poi)
    api(libs.poi.scratchpad)
    api(libs.poi.ooxml.full)
    api(libs.tika.core)

    // AWS
    api(libs.aws.sdk.s3) {
        exclude(group = "commons-logging", module = "commons-logging")
    }

    // Custom
    api(libs.smartdb)

    // YAML & HTML Parser
    api(libs.snakeyaml)
    api(libs.jsoup)

    // JSON Smart
    api(libs.json.smart)

    // Lombok - needs to be available for both modules
    api(libs.lombok)
    api(libs.spotbugs.annotations)
    annotationProcessor(libs.lombok)
}

tasks {
    // Configure resource processing for environment-specific resources
    processResources {
        // Set duplicate handling strategy - environment resources override base resources
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        // Exclude all environment directories first
        exclude("dev/**", "test/**", "pre/**", "prod/**")

        // Include base resources (banner.txt, ip2region.xdb, mapper/, etc.)
        from("src/main/resources") {
            exclude("dev/**", "test/**", "pre/**", "prod/**")
        }

        // Include environment-specific resources with filtering
        from("src/main/resources/$activeEnv") {
            // Filter YAML files for variable substitution
            filesMatching("*.yaml") {
                filter { line ->
                    line.replace("@profiles.active@", activeEnv)
                }
            }
        }
    }
}
