rootProject.name = "sa-parent"

// Enable version catalog
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Include subprojects
include(
    // === Layer 0: Foundation - Cross-cutting concerns (9 modules, formerly sa-common) ===
    "sa-base",
    "sa-base:foundation:core",
    "sa-base:foundation:mq",
    "sa-base:foundation:cache",
    "sa-base:foundation:redis-lock",
    "sa-base:foundation:api-encrypt",
    "sa-base:foundation:captcha",
    "sa-base:foundation:repeat-submit",
    "sa-base:foundation:data-masking",
    "sa-base:foundation:security-protect",

    // === Layer 1: Infrastructure (7 modules, formerly sa-base-*) ===
    // Note: infrastructure:core merged into foundation:core to resolve Gradle circular dependency
    "sa-base:infrastructure:web",
    "sa-base:infrastructure:mybatis",
    "sa-base:infrastructure:redis",
    "sa-base:infrastructure:token",
    "sa-base:infrastructure:datasource",
    "sa-base:infrastructure:swagger",
    "sa-base:infrastructure:devtools",

    // === Layer 2: Business Support (17 modules, formerly sa-base-support) ===
    // Configuration and system management
    "sa-base:support:config",
    "sa-base:support:dict",
    "sa-base:support:reload",

    // File and document management
    "sa-base:support:file",
    "sa-base:support:helpdoc",

    // Job scheduling and monitoring
    "sa-base:support:job",
    "sa-base:support:heartbeat",

    // Logging and auditing
    "sa-base:support:loginlog",
    "sa-base:support:operatelog",
    "sa-base:support:datatracer",

    // User interaction
    "sa-base:support:feedback",
    "sa-base:support:message",
    "sa-base:support:changelog",

    // Utilities
    "sa-base:support:table",
    "sa-base:support:mail",
    "sa-base:support:serialnumber",
    "sa-base:support:codegenerator",

    // === Application Layer ===
    "sa-admin"
)
