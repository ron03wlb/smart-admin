package net.lab1024.sa.support.serialnumber.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 单据序列号 表结构
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-03-25 21:46:07 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_serial_number_record")
public class SerialNumberRecordEntity extends SmartAdminBaseEntity {

  /** 单号id */
  @TableId(type = IdType.NONE)
  private Integer serialNumberId;

  /** 记录日期 */
  private LocalDate recordDate;

  /** 最后更新值 */
  private Long lastNumber;

  /** 上次生成时间 */
  private LocalDateTime lastTime;

  /** 数量 */
  private Long count;
}
