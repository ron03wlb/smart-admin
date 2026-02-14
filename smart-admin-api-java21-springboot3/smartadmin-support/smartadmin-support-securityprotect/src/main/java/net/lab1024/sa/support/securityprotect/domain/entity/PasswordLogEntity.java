package net.lab1024.sa.support.securityprotect.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 密码修改日志
 *
 * @author yandy
 * @since 2024/7/15 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_password_log")
public class PasswordLogEntity extends SmartAdminBaseEntity {

  /** 主键id */
  @TableId(type = IdType.AUTO)
  private Long id;

  private Integer userType;

  private Long userId;

  private String oldPassword;

  private String newPassword;
}
