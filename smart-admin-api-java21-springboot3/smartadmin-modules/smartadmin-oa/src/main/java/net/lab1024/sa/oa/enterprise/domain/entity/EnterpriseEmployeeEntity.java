package net.lab1024.sa.oa.enterprise.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 企业员工
 *
 * @author 1024创新实验室: 罗伊
 * @since 2022/7/28 20:37:15 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_oa_enterprise_employee")
@NoArgsConstructor
public class EnterpriseEmployeeEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long enterpriseEmployeeId;

  /** 企业ID */
  private Long enterpriseId;

  /** 员工 */
  private Long employeeId;

  public EnterpriseEmployeeEntity(Long enterpriseId, Long employeeId) {
    this.enterpriseId = enterpriseId;
    this.employeeId = employeeId;
  }
}
