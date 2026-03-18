rootProject.name = "sa-parent"

// Enable version catalog
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Include subprojects
include(
    // =========================================================================
    // SmartAdmin v4.1.0 Directory Structure
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
    "smartadmin-common:smartadmin-common-tenant",

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

    // === smartadmin-igaming: iGaming Business Modules (8 modules) ===
    "smartadmin-igaming:smartadmin-igaming-common",
    "smartadmin-igaming:smartadmin-igaming-wallet",
    "smartadmin-igaming:smartadmin-igaming-player",
    "smartadmin-igaming:smartadmin-igaming-game",
    "smartadmin-igaming:smartadmin-igaming-activity",
    "smartadmin-igaming:smartadmin-igaming-risk",
    "smartadmin-igaming:smartadmin-igaming-agent",
    "smartadmin-igaming:smartadmin-igaming-integration",

    // === smartadmin-api: API Contract Layer (4 modules) ===
    "smartadmin-api:smartadmin-api-system",
    "smartadmin-api:smartadmin-api-business",
    "smartadmin-api:smartadmin-api-oa",
    "smartadmin-api:smartadmin-api-igaming",

    // === smartadmin-starter: Starter Combinations (2 modules) ===
    "smartadmin-starter:smartadmin-starter-web",
    "smartadmin-starter:smartadmin-starter-all",

    // === smartadmin-app: Unified Application Entry (1 module) ===
    "smartadmin-app"
)
