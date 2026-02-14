package net.lab1024.sa.support.serialnumber.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;
import net.lab1024.sa.support.serialnumber.constant.SerialNumberIdEnum;
import net.lab1024.sa.support.serialnumber.constant.SerialNumberRuleTypeEnum;

/**
 * 单据序列号 定义表
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-25 21:46:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_serial_number")
public class SerialNumberEntity extends SmartAdminBaseEntity {

  /**
   * 主键id
   *
   * @see SerialNumberIdEnum
   */
  @TableId(type = IdType.INPUT)
  private Integer serialNumberId;

  /** 业务 */
  private String businessName;

  /** 格式 */
  private String format;

  /**
   * 生成规则
   *
   * @see SerialNumberRuleTypeEnum
   */
  private String ruleType;

  /** 初始值 */
  private Long initNumber;

  /** 步长随机数范围 */
  private Integer stepRandomRange;

  /** 备注 */
  private String remark;

  /** 上次产生的单号, 默认为空 */
  private Long lastNumber;

  /** 上次产生的单号时间 */
  private LocalDateTime lastTime;
}
