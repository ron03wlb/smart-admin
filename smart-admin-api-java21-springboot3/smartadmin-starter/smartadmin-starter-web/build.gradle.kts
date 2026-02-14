plugins {
    `java-library`
}

description = "SmartAdmin Starter Web - Web application starter without database"

dependencies {
    // BOM 依賴管理
    api(platform(project(":smartadmin-common:smartadmin-common-bom")))

    // ==================== 核心模塊 ====================
    api(project(":smartadmin-common:smartadmin-common-core"))
    api(project(":smartadmin-common:smartadmin-common-web"))
    api(project(":smartadmin-common:smartadmin-common-json"))
    api(project(":smartadmin-common:smartadmin-common-validation"))

    // ==================== 認證與安全 ====================
    api(project(":smartadmin-common:smartadmin-common-token"))       // Sa-Token
    api(project(":smartadmin-common:smartadmin-common-security"))    // XSS/CSRF
    api(project(":smartadmin-common:smartadmin-common-api-encrypt")) // API 加解密

    // ==================== 防護機制 ====================
    api(project(":smartadmin-common:smartadmin-common-repeat-submit")) // 防重複提交
    api(project(":smartadmin-common:smartadmin-common-captcha"))       // 驗證碼
    api(project(":smartadmin-common:smartadmin-common-data-masking"))  // 數據脫敏

    // ==================== API 文檔 ====================
    api(project(":smartadmin-common:smartadmin-common-swagger"))     // Knife4j

    // ==================== 工具類 ====================
    api(project(":smartadmin-common:smartadmin-common-excel"))       // EasyExcel
    api(project(":smartadmin-common:smartadmin-common-ip-geolocation")) // IP 定位
}
