package net.lab1024.sa.support.job.repository.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;
import net.lab1024.sa.support.job.constant.SmartJobTriggerTypeEnum;

/**
 * 定时任务 实体类
 *
 * @author huke
 * @since 2024/6/17 21:30
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_smart_job")
public class SmartJobEntity extends SmartAdminBaseEntity {

  /** 任务id */
  @TableId(type = IdType.AUTO)
  private Integer jobId;

  /** 任务名称 */
  private String jobName;

  /** 执行类 */
  private String jobClass;

  /**
   * 触发类型
   *
   * @see SmartJobTriggerTypeEnum
   */
  private String triggerType;

  /** 触发配置 */
  private String triggerValue;

  /** 定时任务参数 可选 */
  private String param;

  /** 是否启用 */
  private Boolean enabledFlag;

  /** 最后一执行时间 */
  private OffsetDateTime lastExecuteTime;

  /** 最后一次执行记录id */
  private Long lastExecuteLogId;

  /** 备注描述 可选 */
  private String remark;

  /** 排序 */
  private Integer sort;

  /** 是否删除 */
  private Boolean deletedFlag;

  private String updateName;
}
