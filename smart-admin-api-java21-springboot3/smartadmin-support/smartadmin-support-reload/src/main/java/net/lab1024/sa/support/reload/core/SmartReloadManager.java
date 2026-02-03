package net.lab1024.sa.support.reload.core;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.support.reload.core.annoation.SmartReload;
import net.lab1024.sa.support.reload.core.domain.SmartReloadObject;
import net.lab1024.sa.support.reload.core.thread.SmartReloadRunnable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

/**
 * SmartReloadManager 管理器
 *
 * <p>可以在此类中添加 检测任务 以及注册 处理程序
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2015-03-02 19:11:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
public class SmartReloadManager implements BeanPostProcessor, BeanFactoryAware {

  private static final String THREAD_NAME_PREFIX = "smart-reload";
  private static final int THREAD_COUNT = 1;
  private static final int MAX_RELOAD_PARAMS = 1;

  @Value("${reload.interval-seconds}")
  private Integer intervalSeconds;

  private BeanFactory beanFactory;

  private final Map<String, SmartReloadObject> reloadObjectMap = new ConcurrentHashMap<>();

  private ScheduledThreadPoolExecutor threadPoolExecutor;

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  @PostConstruct
  public void init() {
    if (threadPoolExecutor != null) {
      return;
    }

    // 延迟获取 reloadCommand，避免触发早期bean创建
    AbstractSmartReloadCommand reloadCommand =
        beanFactory.getBean(AbstractSmartReloadCommand.class);

    this.threadPoolExecutor =
        new ScheduledThreadPoolExecutor(
            THREAD_COUNT,
            r -> {
              Thread t = new Thread(r, THREAD_NAME_PREFIX);
              if (!t.isDaemon()) {
                t.setDaemon(true);
              }
              return t;
            });
    this.threadPoolExecutor.scheduleWithFixedDelay(
        new SmartReloadRunnable(reloadCommand), 10, this.intervalSeconds, TimeUnit.SECONDS);
    reloadCommand.setReloadManager(this);
  }

  @PreDestroy
  public void shutdown() {
    if (this.threadPoolExecutor != null) {
      this.threadPoolExecutor.shutdownNow();
    }
  }

  @Override
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    Method[] methods = ReflectionUtils.getAllDeclaredMethods(bean.getClass());
    for (Method method : methods) {
      SmartReload smartReload = method.getAnnotation(SmartReload.class);
      if (smartReload == null) {
        continue;
      }
      int paramCount = method.getParameterCount();
      if (paramCount > MAX_RELOAD_PARAMS) {
        if (log.isErrorEnabled()) {
          log.error(
              "<<SmartReloadManager>> register tag reload : "
                  + smartReload.value()
                  + " , param count cannot greater than one !");
        }
        continue;
      }
      String reloadTag = smartReload.value();
      this.register(reloadTag, new SmartReloadObject(bean, method));
    }
    return bean;
  }

  /**
   * 注册reload
   *
   * @param tag
   * @param smartReloadObject
   */
  private void register(String tag, SmartReloadObject smartReloadObject) {
    if (reloadObjectMap.containsKey(tag)) {
      if (log.isErrorEnabled()) {
        log.error(
            "<<SmartReloadManager>> register duplicated tag reload : "
                + tag
                + " , and it will be cover!");
      }
    }
    reloadObjectMap.put(tag, smartReloadObject);
  }

  /**
   * 获取重载对象
   *
   * @return
   */
  public Map<String, SmartReloadObject> getReloadObjectMap() {
    return Collections.unmodifiableMap(this.reloadObjectMap);
  }
}
