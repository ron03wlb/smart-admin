rootProject.name = "sa-parent"

// Enable version catalog
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Include subprojects
include(
    // === Tier 0: Foundation - Core domain objects and utilities ===
    "sa-common",
    "sa-common:core",

    // === SA Common - Shared cross-cutting concerns ===
    "sa-common:mq",
    "sa-common:cache",
    "sa-common:redis-lock",
    "sa-common:api-encrypt",
    "sa-common:captcha",
    "sa-common:repeat-submit",
    "sa-common:data-masking",
    "sa-common:security-protect",

    // === Tier 1: Infrastructure - Base configurations and utilities (7 modules) ===
    "sa-base-core",
    "sa-base-web",
    "sa-base-mybatis",
    "sa-base-redis",
    "sa-base-token",
    "sa-base-datasource",
    "sa-base-swagger",
    "sa-base-devtools",

    // === Tier 2: Support - Business support modules (17 modules) ===
    // Configuration and system management
    "sa-base-support:config",
    "sa-base-support:dict",
    "sa-base-support:reload",

    // File and document management
    "sa-base-support:file",
    "sa-base-support:helpdoc",

    // Job scheduling and monitoring
    "sa-base-support:job",
    "sa-base-support:heartbeat",

    // Logging and auditing
    "sa-base-support:loginlog",
    "sa-base-support:operatelog",
    "sa-base-support:datatracer",

    // User interaction
    "sa-base-support:feedback",
    "sa-base-support:message",
    "sa-base-support:changelog",

    // Utilities
    "sa-base-support:table",
    "sa-base-support:mail",
    "sa-base-support:serialnumber",
    "sa-base-support:codegenerator",

    // === Tier 3: Aggregator - Backward compatibility layer ===
    "sa-base",

    // === Application Layer ===
    "sa-admin"
)
