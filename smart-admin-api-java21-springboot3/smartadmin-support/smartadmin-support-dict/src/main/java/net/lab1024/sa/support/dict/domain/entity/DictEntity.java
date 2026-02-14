package net.lab1024.sa.support.dict.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 数据字典 实体类
 *
 * @author 1024创新实验室-主任-卓大
 * @since 2025-03-25 22:25:04 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_dict")
public class DictEntity extends SmartAdminBaseEntity {

  /** 字典id */
  @TableId(type = IdType.AUTO)
  private Long dictId;

  /** 字典名字 */
  private String dictName;

  /** 字典编码 */
  private String dictCode;

  /** 字典备注 */
  private String remark;

  /** 禁用状态 */
  private Boolean disabledFlag;
}
