package net.lab1024.sa;

import net.lab1024.sa.common.datasource.listener.Ip2RegionListener;
import net.lab1024.sa.common.web.listener.LogVariableListener;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SmartAdmin 統一應用入口
 *
 * <p>v4.1.0 重組架構 - 統一入口應用：
 *
 * <ul>
 *   <li>整合 smartadmin-starter-all（所有 common + support 模塊）
 *   <li>整合 smartadmin-modules（System, Business, OA 模塊）
 *   <li>替代舊的 sa-admin 結構
 *   <li>支持環境切換（dev/test/pre/prod）
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true, exposeProxy = true)
@ComponentScan(SmartAdminApplication.COMPONENT_SCAN)
@MapperScan(value = SmartAdminApplication.COMPONENT_SCAN, annotationClass = Mapper.class)
@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class SmartAdminApplication {

  public static final String COMPONENT_SCAN = "net.lab1024.sa";

  public static void main(String[] args) {
    SpringApplication application = new SpringApplication(SmartAdminApplication.class);
    // 添加日志監聽器，使 log4j2-spring.xml 可以間接讀取到配置文件的屬性
    application.addListeners(new LogVariableListener(), new Ip2RegionListener());
    application.run(args);
  }
}
