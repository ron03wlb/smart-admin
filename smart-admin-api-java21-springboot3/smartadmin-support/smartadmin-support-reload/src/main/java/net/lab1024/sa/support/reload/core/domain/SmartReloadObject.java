package net.lab1024.sa.support.reload.core.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.reflect.Method;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Reload 处理程序的实现方法，用于包装以注解 SmartReload 实现的处理类
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2015-03-02 19:11:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@AllArgsConstructor
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class SmartReloadObject {

  /** 方法对应的实例化对象 */
  private Object reloadObject;

  /** 重新加载执行的方法 */
  private Method method;
}
