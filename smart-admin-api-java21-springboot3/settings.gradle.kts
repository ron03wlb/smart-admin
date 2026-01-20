rootProject.name = "sa-parent"

// Enable version catalog
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Include subprojects
include(
    "sa-base",
    "sa-admin",
    "sa-common",
    "sa-common:mq",
    "sa-common:cache",
    "sa-common:redis-lock",
    "sa-common:api-encrypt",
    "sa-common:captcha",
    "sa-common:repeat-submit",
    "sa-common:data-masking"
)
