package net.lab1024.sa.support.reload.core.thread;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.support.reload.core.AbstractSmartReloadCommand;
import net.lab1024.sa.support.reload.core.domain.SmartReloadItem;
import net.lab1024.sa.support.reload.core.domain.SmartReloadObject;
import net.lab1024.sa.support.reload.core.domain.SmartReloadResult;

/**
 * reload 线程
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2015-03-02 19:11:52 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class SmartReloadRunnable implements Runnable {

  private static final int MAX_PARAM_COUNT = 1;

  private AbstractSmartReloadCommand abstractSmartReloadCommand;

  private boolean isInit = false;

  public SmartReloadRunnable(AbstractSmartReloadCommand abstractSmartReloadCommand) {
    this.abstractSmartReloadCommand = abstractSmartReloadCommand;
  }

  @Override
  public void run() {
    try {
      this.doTask();
    } catch (Exception e) {
      log.error("", e);
    }
  }

  /** 检测Identifier变化，执行reload */
  private void doTask() {
    if (!isInit) {
      this.abstractSmartReloadCommand.init();
      isInit = true;
      return;
    }

    List<SmartReloadItem> smartReloadItemList = this.abstractSmartReloadCommand.readReloadItem();
    java.util.Map<String, String> tagIdentifierMap =
        this.abstractSmartReloadCommand.getTagIdentifierMap();
    for (SmartReloadItem smartReloadItem : smartReloadItemList) {
      String tag = smartReloadItem.getTag();
      String tagIdentifier = smartReloadItem.getIdentification();
      String preTagChangeIdentifier = tagIdentifierMap.get(tag);
      // 数据不一致
      if (preTagChangeIdentifier == null || !preTagChangeIdentifier.equals(tagIdentifier)) {
        this.abstractSmartReloadCommand.putIdentifierMap(tag, tagIdentifier);
        // 执行重新加载此项的动作
        SmartReloadResult smartReloadResult = this.doReload(smartReloadItem);
        this.abstractSmartReloadCommand.handleReloadResult(smartReloadResult);
      }
    }
  }

  /**
   * 方法调用
   *
   * @param smartReloadItem
   * @return
   */
  private SmartReloadResult doReload(SmartReloadItem smartReloadItem) {
    SmartReloadResult result = new SmartReloadResult();
    SmartReloadObject smartReloadObject =
        this.abstractSmartReloadCommand.reloadObject(smartReloadItem.getTag());
    try {
      if (smartReloadObject == null) {
        result.setResult(false);
        result.setException("不能从系统中找到对应的tag：" + smartReloadItem.getTag());
        return result;
      }

      Method method = smartReloadObject.getMethod();
      if (method == null) {
        result.setResult(false);
        result.setException("reload方法不存在");
        return result;
      }

      result.setTag(smartReloadItem.getTag());
      result.setArgs(smartReloadItem.getArgs());
      result.setIdentification(smartReloadItem.getIdentification());
      result.setResult(true);
      int paramCount = method.getParameterCount();
      if (paramCount > MAX_PARAM_COUNT) {
        result.setResult(false);
        result.setException("reload方法" + method.getName() + "参数太多");
        return result;
      }

      if (paramCount == 0) {
        method.invoke(smartReloadObject.getReloadObject());
      } else {
        method.invoke(smartReloadObject.getReloadObject(), smartReloadItem.getArgs());
      }
    } catch (ReflectiveOperationException | RuntimeException throwable) {
      StringWriter sw = new StringWriter();
      try (PrintWriter pw = new PrintWriter(sw)) {
        throwable.printStackTrace(pw);
      }

      result.setResult(false);
      result.setException(sw.toString());
    }
    return result;
  }
}
