plugins {
    java
    alias(libs.plugins.spring.boot)
    id("io.spring.dependency-management")
}

// Get environment from project property (defaults to 'dev')
val activeEnv: String by lazy {
    (project.findProperty("env") as? String) ?: "dev"
}

configurations {
    // Exclude Logback and log4j-to-slf4j globally - using Log4j2 with log4j-slf4j2-impl
    all {
        exclude(group = "ch.qos.logback", module = "logback-classic")
        exclude(group = "ch.qos.logback", module = "logback-core")
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    }
}

dependencies {
    // Dependency on sa-base module
    implementation(project(":sa-base"))

    // Dependency on sa-base-devtools module (code generator)
    implementation(project(":sa-base-devtools"))

    // Dependency on sa-common:mq module
    implementation(project(":sa-common:mq"))

    // Dependency on sa-common:captcha module
    implementation(project(":sa-common:captcha"))

    // Dependency on sa-base-support:table module
    implementation(project(":sa-base-support:table"))

    // Dependency on sa-base-support:feedback module
    implementation(project(":sa-base-support:feedback"))

    // Dependency on sa-base-support:changelog module
    implementation(project(":sa-base-support:changelog"))

    // Dependency on sa-base-support:message module
    implementation(project(":sa-base-support:message"))

    // Phase 2B support modules
    // Dependency on sa-base-support:heartbeat module
    implementation(project(":sa-base-support:heartbeat"))

    // Dependency on sa-base-support:mail module
    implementation(project(":sa-base-support:mail"))

    // Dependency on sa-base-support:serialnumber module
    implementation(project(":sa-base-support:serialnumber"))

    // Dependency on sa-base-support:helpdoc module
    implementation(project(":sa-base-support:helpdoc"))

    // Lombok annotation processor
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.archunit.junit5)
}

tasks {
    // Configure Spring Boot plugin
    springBoot {
        mainClass.set("net.lab1024.sa.admin.AdminApplication")
    }

    // Configure resource processing for environment-specific resources
    processResources {
        // Set duplicate handling strategy - environment resources override base resources
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        // Exclude all environment directories first
        exclude("dev/**", "test/**", "pre/**", "prod/**")

        // Include base resources (mapper/, etc.)
        from("src/main/resources") {
            exclude("dev/**", "test/**", "pre/**", "prod/**")
        }

        // Include environment-specific resources with filtering
        from("src/main/resources/$activeEnv") {
            // Enable filtering for YAML files with @...@ placeholders
            filesMatching("*.yaml") {
                filter { line ->
                    line.replace("@profiles.active@", activeEnv)
                }
            }
        }
    }

    // Configure JAR naming to match Maven pattern: sa-admin-dev-3.0.0.jar
    bootJar {
        // Use custom file name to match Maven's finalName format
        val fileName = "sa-admin-" + activeEnv + "-" + project.version + ".jar"
        archiveFileName.set(fileName)
    }

    // Disable plain jar task (we only want the boot jar)
    named<Jar>("jar") {
        enabled = false
    }
}
