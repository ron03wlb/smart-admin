package net.lab1024.sa.business.category.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.business.category.constant.CategoryTypeEnum;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;
import net.lab1024.sa.common.mybatis.typehandler.BooleanToSmallintTypeHandler;

/**
 * 类目 实体类
 *
 * @author 1024创新实验室: 胡克
 * @since 2021/08/05 21:26:58 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_category")
public class CategoryEntity extends SmartAdminBaseEntity implements Serializable {

  private static final long serialVersionUID = 1L;

  @TableId(type = IdType.AUTO)
  private Long categoryId;

  /** 类目名称 */
  private String categoryName;

  /**
   * 类目 类型
   *
   * @see CategoryTypeEnum
   */
  private Integer categoryType;

  /** 父级类目id */
  private Long parentId;

  /** 是否禁用 */
  @TableField(value = "disabled", typeHandler = BooleanToSmallintTypeHandler.class)
  private Boolean disabledFlag;

  /** 排序 */
  private Integer sort;

  /** 删除状态 */
  @TableField(value = "deleted", typeHandler = BooleanToSmallintTypeHandler.class)
  private Boolean deletedFlag;

  /** 备注 */
  private String remark;
}
