package net.lab1024.sa.base.module.support.serialnumber.service.impl;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.serialnumber.dao.SerialNumberDao;
import net.lab1024.sa.base.module.support.serialnumber.dao.SerialNumberRecordDao;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberEntity;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberGenerateResultBO;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberInfoBO;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberLastGenerateBO;
import net.lab1024.sa.base.module.support.serialnumber.service.SerialNumberBaseService;
import net.lab1024.sa.foundation.domain.exception.BusinessException;

/**
 * 单据序列号 基于mysql锁实现
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-25 21:46:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@SuppressWarnings("PMD.LongVariable")
public class SerialNumberMysqlService extends SerialNumberBaseService {

  public SerialNumberMysqlService(
      SerialNumberRecordDao serialNumberRecordDao, SerialNumberDao serialNumberDao) {
    super(serialNumberRecordDao, serialNumberDao);
  }

  /**
   * 生成序列號列表（基於 MySQL 鎖實現）
   *
   * <p>注意：此方法不包含 @Transactional 註解 根據 SmartAdmin 架構規範，事務由 SerialNumberManager 層處理 此方法在 Manager
   * 的事務上下文中執行，selectForUpdate 會正常工作
   *
   * @param serialNumberInfo 序列號信息
   * @param count 生成數量
   * @return 生成的序列號列表
   */
  @Override
  public List<String> generateSerialNumberList(
      final SerialNumberInfoBO serialNumberInfo, final int count) {
    // // 获取上次的生成结果
    final SerialNumberEntity serialNumberEntity =
        serialNumberDao.selectForUpdate(serialNumberInfo.getSerialNumberId());
    if (serialNumberEntity == null) {
      throw new BusinessException(
          "cannot found SerialNumberId 数据库不存在:" + serialNumberInfo.getSerialNumberId());
    }
    final SerialNumberLastGenerateBO lastGenerateBO =
        SerialNumberLastGenerateBO.builder()
            .lastNumber(serialNumberEntity.getLastNumber())
            .lastTime(serialNumberEntity.getLastTime())
            .serialNumberId(serialNumberEntity.getSerialNumberId())
            .build();

    // 生成
    final SerialNumberGenerateResultBO serialNumberGenerateResult =
        super.loopNumberList(lastGenerateBO, serialNumberInfo, count);

    // 将生成信息保存的内存和数据库
    lastGenerateBO.setLastNumber(serialNumberGenerateResult.getLastNumber());
    lastGenerateBO.setLastTime(serialNumberGenerateResult.getLastTime());
    serialNumberDao.updateLastNumberAndTime(
        serialNumberInfo.getSerialNumberId(),
        serialNumberGenerateResult.getLastNumber(),
        serialNumberGenerateResult.getLastTime());

    // 把生成过程保存到数据库里
    super.saveRecord(serialNumberGenerateResult);

    return formatNumberList(serialNumberGenerateResult, serialNumberInfo);
  }

  @Override
  public void initLastGenerateData(final List<SerialNumberEntity> serialNumberEntityList) {
    // empty
  }
}
