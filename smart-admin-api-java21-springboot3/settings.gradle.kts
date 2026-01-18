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
    "sa-common:d-lock"
)
