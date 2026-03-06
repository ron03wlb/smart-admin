package net.lab1024.sa.system.employee.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;
import net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler;

/**
 * 员工 实体表
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2021-12-09 22:57:49 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_employee")
public class EmployeeEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long employeeId;

  /** 唯一id */
  private String employeeUid;

  /** 登录账号 */
  private String loginName;

  /** 登录密码 */
  private String loginPwd;

  /** 员工名称 */
  private String actualName;

  /** 头像 */
  private String avatar;

  /** 性别 */
  private Integer gender;

  /** 手机号码 */
  private String phone;

  /** 邮箱 */
  private String email;

  /** 部门id */
  private Long departmentId;

  /** 职务级别ID */
  private Long positionId;

  /** 是否为超级管理员: 0 不是，1是 */
  @TableField(typeHandler = BooleanToSmallintTypeHandler.class)
  private Boolean administratorFlag;

  /** 是否被禁用 0否1是 */
  @TableField(typeHandler = BooleanToSmallintTypeHandler.class)
  private Boolean disabledFlag;

  /** 是否删除0否 1是 */
  @TableField(typeHandler = BooleanToSmallintTypeHandler.class)
  private Boolean deletedFlag;

  /** 备注 */
  private String remark;
}
