plugins {
    `java-library`
}

description = "SmartAdmin Starter All - Complete feature starter for development"

dependencies {
    // ==================== Web Starter ====================
    api(project(":smartadmin-starter:smartadmin-starter-web"))

    // ==================== 數據庫 & ORM ====================
    api(project(":smartadmin-common:smartadmin-common-mybatis"))
    api(project(":smartadmin-common:smartadmin-common-datasource"))

    // ==================== 緩存 & Redis ====================
    api(project(":smartadmin-common:smartadmin-common-redis"))
    api(project(":smartadmin-common:smartadmin-common-cache"))
    api(project(":smartadmin-common:smartadmin-common-redis-lock"))

    // ==================== 消息隊列 ====================
    api(project(":smartadmin-common:smartadmin-common-mq"))

    // ==================== 開發工具 ====================
    api(project(":smartadmin-common:smartadmin-common-devtools"))

    // ==================== 多租戶 ====================
    api(project(":smartadmin-common:smartadmin-common-tenant"))

    // ==================== Support 模塊（17 個）====================
    api(project(":smartadmin-support:smartadmin-support-job"))
    api(project(":smartadmin-support:smartadmin-support-config"))
    api(project(":smartadmin-support:smartadmin-support-dict"))
    api(project(":smartadmin-support:smartadmin-support-reload"))
    api(project(":smartadmin-support:smartadmin-support-file"))
    api(project(":smartadmin-support:smartadmin-support-helpdoc"))
    api(project(":smartadmin-support:smartadmin-support-liteflow"))
    api(project(":smartadmin-support:smartadmin-support-loginlog"))
    api(project(":smartadmin-support:smartadmin-support-operatelog"))
    api(project(":smartadmin-support:smartadmin-support-datatracer"))
    api(project(":smartadmin-support:smartadmin-support-feedback"))
    api(project(":smartadmin-support:smartadmin-support-message"))
    api(project(":smartadmin-support:smartadmin-support-changelog"))
    api(project(":smartadmin-support:smartadmin-support-table"))
    api(project(":smartadmin-support:smartadmin-support-mail"))
    api(project(":smartadmin-support:smartadmin-support-serialnumber"))
    api(project(":smartadmin-support:smartadmin-support-codegenerator"))
}
