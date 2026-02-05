package net.lab1024.sa.starter.web.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * SmartAdmin Web Starter 自動配置
 *
 * <p>自動配置包含：
 *
 * <ul>
 *   <li>核心領域對象 (ResponseDTO, ErrorCode, PageResult)
 *   <li>Web 配置 (JSON, CORS, Async, 異常處理)
 *   <li>認證與安全 (Sa-Token, XSS/CSRF 防護, API 加解密)
 *   <li>防護機制 (防重複提交, 驗證碼, 數據脫敏)
 *   <li>API 文檔 (Knife4j)
 *   <li>工具類 (Excel, IP 定位)
 * </ul>
 *
 * <p>不包含：數據庫、緩存、消息隊列（由應用選擇）
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@AutoConfiguration
@ComponentScan(basePackages = "net.lab1024.sa")
public class StarterWebAutoConfiguration {
  // 標記類，依賴傳遞的 AutoConfiguration 生效
}
