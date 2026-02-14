package net.lab1024.sa.support.securityprotect.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 登录失败记录
 *
 * @author 1024创新实验室-主任:卓大
 * @since 2023/10/11 19:29:18 Copyright <a href="https://1024lab.net">1024创新实验室</a>，Since 2012
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_login_fail")
public class LoginFailEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long loginFailId;

  /** 用户id */
  private Long userId;

  /** 用户类型 */
  private Integer userType;

  /** 登录名 */
  private String loginName;

  /** 锁定状态 */
  private Boolean lockFlag;

  /** 登录失败次数 */
  private Integer loginFailCount;

  /** 连续登录失败锁定开始时间 */
  private LocalDateTime loginLockBeginTime;
}
