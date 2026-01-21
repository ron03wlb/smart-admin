package net.lab1024.sa.base.module.support.serialnumber.service.impl;

import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberEntity;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberGenerateResultBO;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberInfoBO;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberLastGenerateBO;
import net.lab1024.sa.base.module.support.serialnumber.service.SerialNumberBaseService;
import org.springframework.stereotype.Service;

/**
 * 单据序列号 基于内存锁实现
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-25 21:46:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
@SuppressWarnings("PMD.LongVariable")
public class SerialNumberInternService extends SerialNumberBaseService {

  /** 按照 serialNumberId 进行锁 */
  private static final Interner<String> POOL = Interners.newStrongInterner();

  private Map<Integer, SerialNumberLastGenerateBO> serialNumberLastGenerateMap =
      new ConcurrentHashMap<>();

  @Override
  public void initLastGenerateData(final List<SerialNumberEntity> serialNumberEntityList) {
    if (serialNumberEntityList == null) {
      return;
    }

    for (final SerialNumberEntity serialNumberEntity : serialNumberEntityList) {
      final SerialNumberLastGenerateBO lastGenerateBO =
          SerialNumberLastGenerateBO.builder()
              .serialNumberId(serialNumberEntity.getSerialNumberId())
              .lastNumber(serialNumberEntity.getLastNumber())
              .lastTime(serialNumberEntity.getLastTime())
              .build();
      serialNumberLastGenerateMap.put(serialNumberEntity.getSerialNumberId(), lastGenerateBO);
    }
  }

  @Override
  public List<String> generateSerialNumberList(
      final SerialNumberInfoBO serialNumberInfo, final int count) {
    final SerialNumberGenerateResultBO serialNumberGenerateResult;
    synchronized (POOL.intern(String.valueOf(serialNumberInfo.getSerialNumberId()))) {

      // 获取上次的生成结果
      final SerialNumberLastGenerateBO lastGenerateBO =
          serialNumberLastGenerateMap.get(serialNumberInfo.getSerialNumberId());

      // 生成
      serialNumberGenerateResult = super.loopNumberList(lastGenerateBO, serialNumberInfo, count);

      // 将生成信息保存的内存和数据库
      lastGenerateBO.setLastNumber(serialNumberGenerateResult.getLastNumber());
      lastGenerateBO.setLastTime(serialNumberGenerateResult.getLastTime());
      serialNumberDao.updateLastNumberAndTime(
          serialNumberInfo.getSerialNumberId(),
          serialNumberGenerateResult.getLastNumber(),
          serialNumberGenerateResult.getLastTime());

      // 把生成过程保存到数据库里
      super.saveRecord(serialNumberGenerateResult);
    }

    return formatNumberList(serialNumberGenerateResult, serialNumberInfo);
  }
}
