package net.lab1024.sa.support.loginlog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 登录日志
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022/07/22 19:46:23 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@TableName("t_login_log")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class LoginLogEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long loginLogId;

  /** 用户id */
  private Long userId;

  /** 用户类型 */
  private Integer userType;

  /** 用户名 */
  private String userName;

  /** 登录ip */
  private String loginIp;

  /** 登录ip地区 */
  private String loginIpRegion;

  /** user-agent */
  private String userAgent;

  /** 备注 */
  private String remark;

  /** 登录设备 */
  private String loginDevice;

  /** 登录类型 */
  private Integer loginResult;
}
