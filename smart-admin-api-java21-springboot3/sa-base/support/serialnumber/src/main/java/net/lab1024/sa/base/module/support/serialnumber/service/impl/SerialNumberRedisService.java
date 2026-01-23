package net.lab1024.sa.base.module.support.serialnumber.service.impl;

import cn.hutool.core.util.RandomUtil;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.serialnumber.constant.SerialNumberRuleTypeEnum;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberEntity;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberGenerateResultBO;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberInfoBO;
import net.lab1024.sa.base.module.support.serialnumber.service.SerialNumberBaseService;
import net.lab1024.sa.foundation.cache.constant.CacheKeyConst;
import net.lab1024.sa.foundation.redislock.RedissonService;
import net.lab1024.sa.foundation.validation.util.SmartEnumUtil;
import net.lab1024.sa.util.SmartDateFormatterEnum;
import net.lab1024.sa.util.SmartLocalDateUtil;
import net.lab1024.sa.util.SmartStringUtil;
import org.redisson.api.RAtomicLong;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 单据序列号 基于redis key-value increase 实现
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-08-03 22:46:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@SuppressWarnings("PMD.LongVariable")
public class SerialNumberRedisService extends SerialNumberBaseService {

  @Resource private RedissonService redissonService;

  private static final int BATCH_GENERATE_THRESHOLD = 1;

  private static final String SERIAL_NUMBER_PREFIX = CacheKeyConst.Support.SERIAL_NUMBER + ":";

  @Override
  public void initLastGenerateData(final List<SerialNumberEntity> serialNumberEntityList) {
    if (serialNumberEntityList == null) {
      return;
    }

    // 设置redis的上次值
    for (final SerialNumberEntity serialNumberEntity : serialNumberEntityList) {
      if (serialNumberEntity.getLastTime() == null) {
        continue;
      }

      final String redisKey =
          generateRedisKeyByDate(
              serialNumberEntity.getSerialNumberId(),
              SmartEnumUtil.getEnumByName(
                  serialNumberEntity.getRuleType().toUpperCase(java.util.Locale.ROOT),
                  SerialNumberRuleTypeEnum.class),
              serialNumberEntity.getLastTime().toLocalDate());

      final RAtomicLong atomicLong = redissonService.getRedissonClient().getAtomicLong(redisKey);
      if (!atomicLong.isExists()) {
        atomicLong.set(serialNumberEntity.getLastNumber());
      }
    }
  }

  /** 每天凌晨一点进行检测； 检测单位数量为3; 3天前、3月前、3年前 */
  @Scheduled(cron = "0 0 1 * * ?")
  public void tryDeleteUnusedRedisKey() {
    for (final SerialNumberInfoBO serialNumberInfoBO : serialNumberMap.values()) {
      final SerialNumberRuleTypeEnum typeEnum = serialNumberInfoBO.getSerialNumberRuleTypeEnum();
      String dateStr = "";
      switch (typeEnum) {
        case DAY -> {
          dateStr =
              SmartLocalDateUtil.format(LocalDate.now().minusDays(3), SmartDateFormatterEnum.YMD);
        }
        case MONTH -> {
          dateStr =
              SmartLocalDateUtil.format(LocalDate.now().minusMonths(3), SmartDateFormatterEnum.YM);
        }
        case YEAR -> {
          dateStr = String.valueOf(LocalDate.now().minusYears(3));
        }
        default -> {
          // No action for NONE or other types
        }
      }
      if (SmartStringUtil.isNotEmpty(dateStr)) {
        final String redisKey =
            SERIAL_NUMBER_PREFIX + serialNumberInfoBO.getSerialNumberId() + ":" + dateStr;
        final RAtomicLong atomicLong = redissonService.getRedissonClient().getAtomicLong(redisKey);
        atomicLong.delete();
      }
    }
  }

  @Override
  public List<String> generateSerialNumberList(
      final SerialNumberInfoBO serialNumberInfo, final int count) {
    // 根据步长，计算 redis 增加值
    final List<Integer> list = new ArrayList<>(count);
    int redisIncrease = 0;
    for (int i = 0; i < count; i++) {
      int stepIncrease = 1;
      final Integer stepRandomRange = serialNumberInfo.getStepRandomRange();
      if (stepRandomRange > BATCH_GENERATE_THRESHOLD) {
        stepIncrease =
            RandomUtil.getSecureRandom().nextInt(1, serialNumberInfo.getStepRandomRange() + 1);
      }
      redisIncrease += stepIncrease;
      list.add(stepIncrease);
    }
    try {
      final String redisKey =
          generateRedisKeyByDate(
              serialNumberInfo.getSerialNumberId(),
              serialNumberInfo.getSerialNumberRuleTypeEnum(),
              LocalDate.now());
      final RAtomicLong atomicLong = redissonService.getRedissonClient().getAtomicLong(redisKey);
      final Long increaseResult = atomicLong.addAndGet(redisIncrease);

      final List<Long> numberList = new ArrayList<>(count);
      Long number = increaseResult;
      for (final Integer i : list) {
        number = number - i;
        numberList.add(number + 1);
      }

      Collections.reverse(numberList);

      final SerialNumberGenerateResultBO serialNumberGenerateResult =
          SerialNumberGenerateResultBO.builder()
              .serialNumberId(serialNumberInfo.getSerialNumberId())
              .lastNumber(increaseResult)
              .lastTime(LocalDateTime.now())
              .numberList(numberList)
              .isReset(false)
              .build();

      // 将生成信息保存的内存和数据库
      serialNumberDao.updateLastNumberAndTime(
          serialNumberInfo.getSerialNumberId(),
          serialNumberGenerateResult.getLastNumber(),
          serialNumberGenerateResult.getLastTime());

      // 把生成过程保存到数据库里
      super.saveRecord(serialNumberGenerateResult);
      return formatNumberList(serialNumberGenerateResult, serialNumberInfo);
    } catch (Exception | Error e) {
      if (log.isErrorEnabled()) {
        log.error(e.getMessage(), e);
      }
      throw e;
    }
  }

  private String generateRedisKeyByDate(
      final Integer serialNumberId,
      final SerialNumberRuleTypeEnum serialNumberRuleTypeEnum,
      final LocalDate localDate) {
    return switch (serialNumberRuleTypeEnum) {
      case DAY -> {
        final String dayStr = SmartLocalDateUtil.format(localDate, SmartDateFormatterEnum.YMD);
        yield SERIAL_NUMBER_PREFIX + serialNumberId + ":" + dayStr;
      }
      case MONTH -> {
        final String monthStr = SmartLocalDateUtil.format(localDate, SmartDateFormatterEnum.YM);
        yield SERIAL_NUMBER_PREFIX + serialNumberId + ":" + monthStr;
      }
      case YEAR -> {
        final String yearStr = String.valueOf(localDate.getYear());
        yield SERIAL_NUMBER_PREFIX + serialNumberId + ":" + yearStr;
      }
      case NONE -> SERIAL_NUMBER_PREFIX + serialNumberId;
      default ->
          throw new IllegalArgumentException("Unsupported rule type: " + serialNumberRuleTypeEnum);
    };
  }
}
