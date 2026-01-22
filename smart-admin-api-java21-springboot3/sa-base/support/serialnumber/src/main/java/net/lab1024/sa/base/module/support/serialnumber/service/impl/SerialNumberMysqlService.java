package net.lab1024.sa.base.module.support.serialnumber.service.impl;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberEntity;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberGenerateResultBO;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberInfoBO;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberLastGenerateBO;
import net.lab1024.sa.base.module.support.serialnumber.service.SerialNumberBaseService;
import net.lab1024.sa.foundation.domain.exception.BusinessException;
import org.springframework.transaction.annotation.Transactional;

/**
 * 单据序列号 基于mysql锁实现
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-25 21:46:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@SuppressWarnings("PMD.LongVariable")
public class SerialNumberMysqlService extends SerialNumberBaseService {

  @Override
  @Transactional(rollbackFor = Throwable.class)
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
