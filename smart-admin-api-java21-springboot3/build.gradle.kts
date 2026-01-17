plugins {
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
    alias(libs.plugins.spotbugs) apply false
    alias(libs.plugins.errorprone) apply false
    alias(libs.plugins.sonarqube) apply false
    alias(libs.plugins.spotless) apply false
    java
}

allprojects {
    group = "net.lab1024"
    version = "3.0.0"

    repositories {
        mavenCentral()
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/spring") }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "checkstyle")
    apply(plugin = "pmd")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "net.ltgt.errorprone")
    apply(plugin = "jacoco")
    apply(plugin = "org.sonarqube")
    apply(plugin = "com.diffplug.spotless")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        // Maven's -parameters flag for parameter name retention
        options.compilerArgs.add("-parameters")
        dependsOn("spotlessApply")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    the<io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension>().apply {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.4")
            mavenBom("com.baomidou:mybatis-plus-bom:3.5.12")
        }
    }

    // Checkstyle Configuration
    configure<CheckstyleExtension> {
        val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        toolVersion = libs.findVersion("checkstyle").get().toString()
        isIgnoreFailures = false
        maxWarnings = 0
        // configFile will be created in the next step
        configFile = rootProject.file("config/checkstyle/checkstyle.xml")
    }

    // PMD Configuration
    configure<PmdExtension> {
        val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        toolVersion = libs.findVersion("pmd").get().toString()
        isIgnoreFailures = false
        ruleSets = listOf("category/java/errorprone.xml", "category/java/bestpractices.xml")
    }

    // SpotBugs Configuration
    configure<com.github.spotbugs.snom.SpotBugsExtension> {
        val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        toolVersion.set(libs.findVersion("spotbugs").get().toString())
        ignoreFailures.set(false)
        effort.set(com.github.spotbugs.snom.Effort.MAX)
        reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
    }

    // Error Prone Configuration
    dependencies {
        val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        "errorprone"(libs.findLibrary("error-prone-core").get())
    }

    // JaCoCo Configuration
    configure<JacocoPluginExtension> {
        val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
        toolVersion = libs.findVersion("jacoco").get().toString()
    }

    tasks.withType<JacocoReport> {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    // Spotless Configuration
    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            googleJavaFormat()
            removeUnusedImports()
            trimTrailingWhitespace()
            endWithNewline()
        }
    }
}

// Git Hooks Task
tasks.register("installGitHooks") {
    doLast {
        val hooksSourceDir = file("config/git")
        val gitHooksDir = file("../.git/hooks")
        
        if (hooksSourceDir.exists()) {
            hooksSourceDir.listFiles()?.forEach { hookFile ->
                val targetFile = File(gitHooksDir, hookFile.name)
                hookFile.copyTo(targetFile, overwrite = true)
                targetFile.setExecutable(true)
            }
            println("Git hooks installed successfully.")
        } else {
             // Create pre-commit hook if it doesn't exist
            val preCommitHook = File(gitHooksDir, "pre-commit")
            preCommitHook.writeText("""
                #!/bin/sh
                echo "Running Spotless in smart-admin-api-java21-springboot3..."
                cd smart-admin-api-java21-springboot3 && ./gradlew spotlessApply
            """.trimIndent())
            preCommitHook.setExecutable(true)
            println("Default pre-commit hook created.")
        }
    }
}

// Ensure git hooks are installed during project sync or build
tasks.named("build") {
    dependsOn("installGitHooks")
}
