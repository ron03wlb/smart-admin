package net.lab1024.sa.common.tenant.config;

import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.tenant.TenantProperties;
import net.lab1024.sa.common.tenant.filter.TenantFilter;
import net.lab1024.sa.common.tenant.interceptor.RlsSessionInterceptor;
import net.lab1024.sa.common.tenant.service.TenantService;
import net.lab1024.sa.common.tenant.taskdecorator.TenantTaskDecorator;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Auto-configuration for multi-tenant infrastructure.
 *
 * <p>Conditionally enabled via {@code tenant.enabled=true} in application properties. Registers
 * TenantFilter, TenantTaskDecorator, and enables TenantProperties.
 *
 * @author SmartAdmin
 * @since 2026-02-15
 */
@Configuration
@ConditionalOnProperty(name = "tenant.enabled", havingValue = "true", matchIfMissing = false)
@EnableConfigurationProperties(TenantProperties.class)
@RequiredArgsConstructor
public class TenantAutoConfiguration {

  private final TenantProperties tenantProperties;

  @Bean
  public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(
      TenantService tenantService) {
    FilterRegistrationBean<TenantFilter> registrationBean = new FilterRegistrationBean<>();
    registrationBean.setFilter(new TenantFilter(tenantService, tenantProperties));
    registrationBean.addUrlPatterns("/*");
    registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
    registrationBean.setName("tenantFilter");
    return registrationBean;
  }

  @Bean
  @ConditionalOnProperty(name = "tenant.rls.enabled", havingValue = "true")
  public RlsSessionInterceptor rlsSessionInterceptor() {
    return new RlsSessionInterceptor();
  }

  @Bean
  public BeanPostProcessor tenantTaskDecoratorPostProcessor() {
    return new BeanPostProcessor() {
      @Override
      public Object postProcessAfterInitialization(Object bean, String beanName)
          throws BeansException {
        if (bean instanceof ThreadPoolTaskExecutor executor) {
          executor.setTaskDecorator(new TenantTaskDecorator());
        }
        return bean;
      }
    };
  }
}
