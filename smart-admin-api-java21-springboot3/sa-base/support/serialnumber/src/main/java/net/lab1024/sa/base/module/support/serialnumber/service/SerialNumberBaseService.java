package net.lab1024.sa.base.module.support.serialnumber.service;

import com.google.common.collect.Lists;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.lab1024.sa.base.module.support.serialnumber.constant.SerialNumberIdEnum;
import net.lab1024.sa.base.module.support.serialnumber.constant.SerialNumberRuleTypeEnum;
import net.lab1024.sa.base.module.support.serialnumber.dao.SerialNumberDao;
import net.lab1024.sa.base.module.support.serialnumber.dao.SerialNumberRecordDao;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberEntity;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberGenerateResultBO;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberInfoBO;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberLastGenerateBO;
import net.lab1024.sa.base.module.support.serialnumber.domain.SerialNumberRecordEntity;
import net.lab1024.sa.common.core.exception.BusinessException;
import net.lab1024.sa.foundation.validation.util.SmartEnumUtil;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 单据序列号 基类
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-25 21:46:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@SuppressWarnings({"PMD.LongVariable", "PMD.AvoidInstantiatingObjectsInLoops"})
public abstract class SerialNumberBaseService implements SerialNumberService {

  @Resource protected SerialNumberRecordDao serialNumberRecordDao;

  @Resource protected SerialNumberDao serialNumberDao;

  protected Map<Integer, SerialNumberInfoBO> serialNumberMap = new ConcurrentHashMap<>();

  private static final int MIN_STEP_RANDOM_RANGE = 1;

  private static final int MIN_STEP_FOR_RANDOM = 1;

  public abstract List<String> generateSerialNumberList(SerialNumberInfoBO serialNumber, int count);

  @PostConstruct
  void init() {
    final List<SerialNumberEntity> serialNumberEntityList = serialNumberDao.selectList(null);
    if (serialNumberEntityList == null) {
      return;
    }
    for (final SerialNumberEntity serialNumberEntity : serialNumberEntityList) {
      final SerialNumberRuleTypeEnum ruleTypeEnum =
          SmartEnumUtil.getEnumByName(
              serialNumberEntity.getRuleType().toUpperCase(java.util.Locale.ROOT),
              SerialNumberRuleTypeEnum.class);
      if (ruleTypeEnum == null) {
        throw new ExceptionInInitializerError(
            "cannot find rule type , id : " + serialNumberEntity.getSerialNumberId());
      }

      final String format = serialNumberEntity.getFormat();
      final int startIndex = format.indexOf("[n");
      final int endIndex = format.indexOf("n]");
      if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) {
        throw new ExceptionInInitializerError(
            "[nnn] 配置错误，请仔细查看 id : " + serialNumberEntity.getSerialNumberId());
      }

      if (serialNumberEntity.getStepRandomRange() < MIN_STEP_RANDOM_RANGE) {
        throw new ExceptionInInitializerError(
            "random step range must greater than 1 " + serialNumberEntity.getSerialNumberId());
      }

      final String numberFormat = format.substring(startIndex + 1, endIndex + 1);

      final SerialNumberInfoBO serialNumberInfoBO =
          SerialNumberInfoBO.builder()
              .serialNumberId(serialNumberEntity.getSerialNumberId())
              .serialNumberRuleTypeEnum(ruleTypeEnum)
              .initNumber(serialNumberEntity.getInitNumber())
              .format(serialNumberEntity.getFormat())
              .stepRandomRange(serialNumberEntity.getStepRandomRange())
              .haveDayFlag(format.contains(SerialNumberRuleTypeEnum.DAY.getValue()))
              .haveMonthFlag(format.contains(SerialNumberRuleTypeEnum.MONTH.getValue()))
              .haveYearFlag(format.contains(SerialNumberRuleTypeEnum.YEAR.getValue()))
              .numberCount(endIndex - startIndex)
              .numberFormat("\\[" + numberFormat + "\\]")
              .build();

      this.serialNumberMap.put(serialNumberEntity.getSerialNumberId(), serialNumberInfoBO);
    }

    // 初始化数据
    initLastGenerateData(serialNumberEntityList);
  }

  /**
   * 初始化上次生成的数据
   *
   * @param serialNumberEntityList
   */
  public abstract void initLastGenerateData(List<SerialNumberEntity> serialNumberEntityList);

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public String generate(final SerialNumberIdEnum serialNumberIdEnum) {
    final List<String> generateList = this.generate(serialNumberIdEnum, 1);
    if (generateList == null || generateList.isEmpty()) {
      throw new BusinessException("cannot generate : " + serialNumberIdEnum.toString());
    }
    return generateList.get(0);
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public List<String> generate(final SerialNumberIdEnum serialNumberIdEnum, final int count) {
    final SerialNumberInfoBO serialNumberInfoBO =
        serialNumberMap.get(serialNumberIdEnum.getSerialNumberId());
    if (serialNumberInfoBO == null) {
      throw new BusinessException("cannot found SerialNumberId : " + serialNumberIdEnum.toString());
    }
    return this.generateSerialNumberList(serialNumberInfoBO, count);
  }

  /**
   * 循环生成 number 集合
   *
   * @param lastGenerate
   * @param serialNumberInfo
   * @param count
   * @return
   */
  protected SerialNumberGenerateResultBO loopNumberList(
      final SerialNumberLastGenerateBO lastGenerate,
      final SerialNumberInfoBO serialNumberInfo,
      final int count) {
    Long lastNumber = lastGenerate.getLastNumber();
    boolean isReset = false;
    if (isResetInitNumber(lastGenerate, serialNumberInfo)) {
      lastNumber = serialNumberInfo.getInitNumber();
      isReset = true;
    }

    final List<Long> numberList = Lists.newArrayListWithCapacity(count);
    for (int i = 0; i < count; i++) {
      final Integer stepRandomRange = serialNumberInfo.getStepRandomRange();
      if (stepRandomRange > MIN_STEP_FOR_RANDOM) {
        lastNumber = lastNumber + RandomUtils.nextInt(1, stepRandomRange + 1);
      } else {
        lastNumber = lastNumber + 1;
      }

      numberList.add(lastNumber);
    }

    return SerialNumberGenerateResultBO.builder()
        .serialNumberId(serialNumberInfo.getSerialNumberId())
        .lastNumber(lastNumber)
        .lastTime(LocalDateTime.now())
        .numberList(numberList)
        .isReset(isReset)
        .build();
  }

  protected void saveRecord(final SerialNumberGenerateResultBO resultBO) {
    final Long effectRows =
        serialNumberRecordDao.updateRecord(
            resultBO.getSerialNumberId(),
            resultBO.getLastTime().toLocalDate(),
            resultBO.getLastNumber(),
            resultBO.getNumberList().size());

    // 需要插入
    if (effectRows == null || effectRows == 0) {
      final SerialNumberRecordEntity recordEntity =
          SerialNumberRecordEntity.builder()
              .serialNumberId(resultBO.getSerialNumberId())
              .recordDate(LocalDate.now())
              .lastTime(resultBO.getLastTime())
              .lastNumber(resultBO.getLastNumber())
              .count((long) resultBO.getNumberList().size())
              .build();
      serialNumberRecordDao.insert(recordEntity);
    }
  }

  /**
   * 若不在规则周期内，重制初始值
   *
   * @return
   */
  private boolean isResetInitNumber(
      final SerialNumberLastGenerateBO lastGenerate, final SerialNumberInfoBO serialNumberInfo) {
    final LocalDateTime lastTime = lastGenerate.getLastTime();
    if (lastTime == null) {
      return true;
    }

    final SerialNumberRuleTypeEnum serialNumberRuleTypeEnum =
        serialNumberInfo.getSerialNumberRuleTypeEnum();
    final int lastTimeYear = lastTime.getYear();
    final int lastTimeMonth = lastTime.getMonthValue();
    final int lastTimeDay = lastTime.getDayOfYear();

    final LocalDateTime now = LocalDateTime.now();

    switch (serialNumberRuleTypeEnum) {
      case YEAR:
        return lastTimeYear != now.getYear();
      case MONTH:
        return lastTimeYear != now.getYear() || lastTimeMonth != now.getMonthValue();
      case DAY:
        return lastTimeYear != now.getYear() || lastTimeDay != now.getDayOfYear();
      default:
        return false;
    }
  }

  /** 替换特殊rule，即替换[yyyy][mm][dd][nnn]等规则 */
  protected List<String> formatNumberList(
      final SerialNumberGenerateResultBO generateResult,
      final SerialNumberInfoBO serialNumberInfo) {

    /** 第一步：替换年、月、日 */
    final LocalDate lastTime = generateResult.getLastTime().toLocalDate();
    final String year = String.valueOf(lastTime.getYear());
    final String month =
        lastTime.getMonthValue() > 9
            ? String.valueOf(lastTime.getMonthValue())
            : "0" + lastTime.getMonthValue();
    final String day =
        lastTime.getDayOfMonth() > 9
            ? String.valueOf(lastTime.getDayOfMonth())
            : "0" + lastTime.getDayOfMonth();

    // 把年月日替换
    String format = serialNumberInfo.getFormat();

    if (serialNumberInfo.getHaveYearFlag()) {
      format = format.replaceAll(SerialNumberRuleTypeEnum.YEAR.getRegex(), year);
    }
    if (serialNumberInfo.getHaveMonthFlag()) {
      format = format.replaceAll(SerialNumberRuleTypeEnum.MONTH.getRegex(), month);
    }
    if (serialNumberInfo.getHaveDayFlag()) {
      format = format.replaceAll(SerialNumberRuleTypeEnum.DAY.getRegex(), day);
    }

    /** 第二步：替换数字 */
    final List<String> numberList =
        Lists.newArrayListWithCapacity(generateResult.getNumberList().size());
    for (final Long number : generateResult.getNumberList()) {
      final StringBuilder numberStringBuilder = new StringBuilder();
      final int currentNumberCount = String.valueOf(number).length();
      // 数量不够，前面补0
      if (serialNumberInfo.getNumberCount() > currentNumberCount) {
        final int remain = serialNumberInfo.getNumberCount() - currentNumberCount;
        for (int i = 0; i < remain; i++) {
          numberStringBuilder.append(0);
        }
      }
      numberStringBuilder.append(number);
      // 最终替换
      final String finalNumber =
          format.replaceAll(serialNumberInfo.getNumberFormat(), numberStringBuilder.toString());
      numberList.add(finalNumber);
    }
    return numberList;
  }
}
