package net.lab1024.sa.support.serialnumber.manager;

import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.support.serialnumber.constant.SerialNumberIdEnum;
import net.lab1024.sa.support.serialnumber.domain.SerialNumberInfoBO;
import net.lab1024.sa.support.serialnumber.service.SerialNumberBaseService;
import net.lab1024.sa.common.core.domain.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 單據序列號 Manager 層 - 處理事務邏輯
 *
 * <p>根據 SmartAdmin 架構規範，@Transactional 只能在 Manager 層使用
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-25 21:46:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@RequiredArgsConstructor
public class SerialNumberManager {

  private final SerialNumberBaseService serialNumberBaseService;

  /**
   * 生成單個序列號（帶事務）
   *
   * @param serialNumberIdEnum 序列號ID枚舉
   * @return 生成的序列號
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
  public String generateTransaction(final SerialNumberIdEnum serialNumberIdEnum) {
    final List<String> generateList = this.generateTransaction(serialNumberIdEnum, 1);
    if (generateList == null || generateList.isEmpty()) {
      throw new BusinessException("cannot generate : " + serialNumberIdEnum.toString());
    }
    return generateList.get(0);
  }

  /**
   * 批量生成序列號（帶事務）
   *
   * @param serialNumberIdEnum 序列號ID枚舉
   * @param count 生成數量
   * @return 生成的序列號列表
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Throwable.class)
  public List<String> generateTransaction(
      final SerialNumberIdEnum serialNumberIdEnum, final int count) {
    final SerialNumberInfoBO serialNumberInfoBO =
        serialNumberBaseService.getSerialNumberInfo(serialNumberIdEnum);
    if (serialNumberInfoBO == null) {
      throw new BusinessException("cannot found SerialNumberId : " + serialNumberIdEnum.toString());
    }
    return serialNumberBaseService.generateSerialNumberList(serialNumberInfoBO, count);
  }
}
