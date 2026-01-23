package net.lab1024.sa.base.datasource.config;

import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.mybatis.handler.MybatisPlusFillHandler;
import net.lab1024.sa.domain.DataScopePlugin;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * 数据源配置 - PostgreSQL + HikariCP
 *
 * <p>使用 Spring Boot 自动配置的 HikariCP 数据源，无需手动配置 Druid。 数据源通过 application.yaml 中的
 * spring.datasource.hikari 配置。
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2017-11-28 15:21:10 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

  private final DataSource dataSource;

  private final MybatisPlusInterceptor paginationInterceptor;

  private final DataScopePlugin dataScopePlugin;

  @Bean
  public SqlSessionFactory sqlSessionFactory() throws Exception {
    MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
    factoryBean.setDataSource(dataSource);
    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    Resource[] resources = resolver.getResources("classpath*:/mapper/**/*.xml");
    factoryBean.setMapperLocations(resources);

    // 设置 MyBatis-Plus 分页插件 注意此处myBatisPlugin一定要放在后面
    List<Interceptor> pluginsList = new ArrayList<>();
    pluginsList.add(paginationInterceptor);
    if (dataScopePlugin != null) {
      pluginsList.add(dataScopePlugin);
    }
    factoryBean.setPlugins(pluginsList.toArray(new Interceptor[0]));
    // 添加字段自动填充处理
    factoryBean.setGlobalConfig(
        new GlobalConfig().setBanner(false).setMetaObjectHandler(new MybatisPlusFillHandler()));

    return factoryBean.getObject();
  }
}
