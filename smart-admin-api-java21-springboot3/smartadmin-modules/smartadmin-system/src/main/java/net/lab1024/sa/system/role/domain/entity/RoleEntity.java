package net.lab1024.sa.system.role.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 角色
 *
 * @author 1024创新实验室: 胡克
 * @since 2022-03-07 18:54:42 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@TableName("t_role")
public class RoleEntity {
  /** 主键id */
  @TableId(type = IdType.AUTO)
  private Long roleId;

  /** 角色名称 */
  private String roleName;

  /** 角色编码 */
  private String roleCode;

  /** 角色备注 */
  private String remark;

  /** 更新时间 */
  private LocalDateTime updateTime;

  /** 创建时间 */
  private LocalDateTime createTime;
}
