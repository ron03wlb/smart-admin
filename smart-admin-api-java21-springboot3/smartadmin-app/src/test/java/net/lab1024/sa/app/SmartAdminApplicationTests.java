package net.lab1024.sa.app;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * SmartAdmin 應用啟動測試
 *
 * <p>驗證 Spring Context 正常加載和所有 AutoConfiguration 生效
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class SmartAdminApplicationTests {

  @Test
  void contextLoads() {
    // 驗證 Spring Context 正常加載
    // 驗證所有 AutoConfiguration 生效
  }
}
