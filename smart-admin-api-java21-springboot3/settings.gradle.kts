rootProject.name = "sa-parent"

// Enable version catalog
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Include subprojects
include(
    "sa-base",
    "sa-admin",
    "sa-common",
    // Existing sa-common modules
    "sa-common:mq",
    "sa-common:cache",
    "sa-common:redis-lock",
    "sa-common:api-encrypt",
    "sa-common:captcha",
    "sa-common:repeat-submit",
    "sa-common:data-masking",
    "sa-common:security-protect",
    // New modules - Phase 1 (core infrastructure)
    "sa-common:core",
    // sa-base infrastructure modules - Phase 2
    "sa-base-core",
    // sa-base devtools module - Code generator and development utilities
    "sa-base-devtools",
    // sa-base support modules - Extracted support functionality
    "sa-base-support:table",
    "sa-base-support:feedback",
    "sa-base-support:changelog",
    "sa-base-support:message"
)
