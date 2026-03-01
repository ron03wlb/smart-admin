package net.lab1024.sa.igaming.game.config;

import net.lab1024.sa.igaming.game.interceptor.CallbackTenantInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * iGaming WebMvc configuration — registers interceptors for GP/PSP callback request handling.
 *
 * @author iGaming Team
 * @since 2026-02-17
 */
@Configuration
public class IgamingWebMvcConfig implements WebMvcConfigurer {

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(new CallbackTenantInterceptor())
        .addPathPatterns("/igaming/game/callback/**", "/igaming/payment/callback/**");
  }
}
