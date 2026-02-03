package net.lab1024.sa.support.dict.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 字典数据表 实体类
 *
 * @author 1024创新实验室-主任-卓大
 * @since 2025-03-25 23:12:59 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@TableName("t_dict_data")
public class DictDataEntity {

  /** 字典数据id */
  @TableId(type = IdType.AUTO)
  private Long dictDataId;

  /** 字典id */
  private Long dictId;

  /** 字典项值 */
  private String dataValue;

  /** 字典项显示名称 */
  private String dataLabel;

  /** 备注 */
  private String remark;

  /** 排序（越大越靠前） */
  private Integer sortOrder;

  /** 禁用状态 */
  private Boolean disabledFlag;

  /** 创建时间 */
  private LocalDateTime createTime;

  /** 更新时间 */
  private LocalDateTime updateTime;
}
