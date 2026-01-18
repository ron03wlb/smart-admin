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
    testImplementation {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
    testRuntimeOnly {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-logging")
    }
}

dependencies {
    // Dependency on sa-base module
    implementation(project(":sa-base"))

    // Dependency on sa-common:mq module
    implementation(project(":sa-common:mq"))

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
