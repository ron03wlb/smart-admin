package net.lab1024.sa.system.department.domain.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 部门实体类
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2022-01-12 20:37:48 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_department")
public class DepartmentEntity extends SmartAdminBaseEntity {

  /** 主键id */
  @TableId(type = IdType.AUTO)
  private Long departmentId;

  /** 部门名称 */
  private String departmentName;

  /** 负责人员工 id */
  @TableField(updateStrategy = FieldStrategy.NEVER)
  private Long managerId;

  /** 部门父级id */
  private Long parentId;

  /** 排序 */
  private Integer sort;
}
