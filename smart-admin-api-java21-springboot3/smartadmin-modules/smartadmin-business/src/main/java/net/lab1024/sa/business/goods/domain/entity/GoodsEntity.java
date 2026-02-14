package net.lab1024.sa.business.goods.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.common.mybatis.domain.SmartAdminBaseEntity;

/**
 * 商品 实体类
 *
 * @author 1024创新实验室: 胡克
 * @since 2021-10-25 20:26:54 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_goods")
public class GoodsEntity extends SmartAdminBaseEntity {

  @TableId(type = IdType.AUTO)
  private Long goodsId;

  /** 商品状态:[1:预约中, 2:售卖中, 3:售罄] */
  private Integer goodsStatus;

  /** 商品分类 */
  private Long categoryId;

  /** 商品名称 */
  private String goodsName;

  /** 产地 */
  private String place;

  /** 商品价格 */
  private BigDecimal price;

  /** 上架状态 */
  private Boolean shelvesFlag;

  /** 删除状态 */
  private Boolean deletedFlag;

  /** 备注 */
  private String remark;
}
