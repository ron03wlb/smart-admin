package net.lab1024.sa.base.mybatis.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
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

  /** 分页插件 */
  @Bean
  public MybatisPlusInterceptor paginationInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.POSTGRE_SQL));
    return interceptor;
  }
}
