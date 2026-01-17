package net.lab1024.sa.admin;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@org.springframework.test.context.ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Integration test - requires database connection")
class AdminApplicationTest {

  private static final Logger log = LoggerFactory.getLogger(AdminApplicationTest.class);

  @Autowired private ApplicationContext applicationContext;

  @BeforeEach
  public void before() {
    log.info("----------------------- 测试开始 -----------------------");
  }

  @AfterEach
  public void after() {
    log.info("----------------------- 测试结束 -----------------------");
  }

  @Test
  void contextLoads() {
    assertNotNull(applicationContext, "ApplicationContext should not be null");
  }
}
