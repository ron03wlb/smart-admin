rootProject.name = "sa-parent"

// Enable version catalog
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Include subprojects
include(
    // === Layer 0: Foundation - Cross-cutting concerns (14 modules, formerly sa-common) ===
    "sa-base",
    "sa-base:foundation:domain",
    "sa-base:foundation:validation",
    "sa-base:foundation:json",
    "sa-base:foundation:ip-geolocation",
    "sa-base:foundation:excel",
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

    // Workflow and rules engine
    "sa-base:support:liteflow",

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
    "sa-admin",

    // =========================================================================
    // NEW STRUCTURE (v4.1.0 Directory Restructure - Week 1)
    // =========================================================================

    // === smartadmin-common: Public Foundation (21 modules) ===
    "smartadmin-common:smartadmin-common-bom",
    "smartadmin-common:smartadmin-common-core",
    "smartadmin-common:smartadmin-common-web",
    "smartadmin-common:smartadmin-common-mybatis",
    "smartadmin-common:smartadmin-common-redis",
    "smartadmin-common:smartadmin-common-token",
    "smartadmin-common:smartadmin-common-datasource",
    "smartadmin-common:smartadmin-common-swagger",
    "smartadmin-common:smartadmin-common-devtools",
    "smartadmin-common:smartadmin-common-validation",
    "smartadmin-common:smartadmin-common-json",
    "smartadmin-common:smartadmin-common-excel",
    "smartadmin-common:smartadmin-common-ip-geolocation",
    "smartadmin-common:smartadmin-common-cache",
    "smartadmin-common:smartadmin-common-mq",
    "smartadmin-common:smartadmin-common-redis-lock",
    "smartadmin-common:smartadmin-common-api-encrypt",
    "smartadmin-common:smartadmin-common-captcha",
    "smartadmin-common:smartadmin-common-repeat-submit",
    "smartadmin-common:smartadmin-common-data-masking",
    "smartadmin-common:smartadmin-common-security",

    // === smartadmin-support: Business Support (17 modules) ===
    "smartadmin-support:smartadmin-support-config",
    "smartadmin-support:smartadmin-support-dict",
    "smartadmin-support:smartadmin-support-reload",
    "smartadmin-support:smartadmin-support-file",
    "smartadmin-support:smartadmin-support-helpdoc",
    "smartadmin-support:smartadmin-support-job",
    "smartadmin-support:smartadmin-support-heartbeat",
    "smartadmin-support:smartadmin-support-liteflow",
    "smartadmin-support:smartadmin-support-loginlog",
    "smartadmin-support:smartadmin-support-operatelog",
    "smartadmin-support:smartadmin-support-datatracer",
    "smartadmin-support:smartadmin-support-feedback",
    "smartadmin-support:smartadmin-support-message",
    "smartadmin-support:smartadmin-support-changelog",
    "smartadmin-support:smartadmin-support-table",
    "smartadmin-support:smartadmin-support-mail",
    "smartadmin-support:smartadmin-support-serialnumber",
    "smartadmin-support:smartadmin-support-securityprotect",
    "smartadmin-support:smartadmin-support-codegenerator",

    // === smartadmin-modules: Business Modules (3 modules) ===
    "smartadmin-modules:smartadmin-system",
    "smartadmin-modules:smartadmin-business",
    "smartadmin-modules:smartadmin-oa",

    // === smartadmin-api: API Contract Layer (3 modules) ===
    "smartadmin-api:smartadmin-api-system",
    "smartadmin-api:smartadmin-api-business",
    "smartadmin-api:smartadmin-api-oa",

    // === smartadmin-starter: Starter Combinations (1 module) ===
    "smartadmin-starter",

    // === smartadmin-app: Unified Application Entry (1 module) ===
    "smartadmin-app"
)
