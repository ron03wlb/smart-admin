package net.lab1024.sa.common.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.lab1024.sa.common.mybatis.handler.SmartTenantLineHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * mp 插件
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2021-09-02 20:21:10 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EnableTransactionManagement
@Configuration
public class MybatisPlusConfig {

  @Value("${tenant.enabled:false}")
  private boolean tenantEnabled;

  /** 分页插件 + 多租户插件 + 乐观锁插件 */
  @Bean
  public MybatisPlusInterceptor paginationInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

    if (tenantEnabled) {
      TenantLineInnerInterceptor tenantInterceptor = new TenantLineInnerInterceptor();
      tenantInterceptor.setTenantLineHandler(new SmartTenantLineHandler());
      interceptor.addInnerInterceptor(tenantInterceptor);
    }

    // 乐观锁插件 (optimistic locking for @Version annotated entities)
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
    return interceptor;
  }
}
