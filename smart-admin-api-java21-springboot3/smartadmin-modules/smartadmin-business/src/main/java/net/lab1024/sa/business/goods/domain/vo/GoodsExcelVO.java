package net.lab1024.sa.business.goods.domain.vo;

import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * excel商品
 *
 * @author 1024创新实验室: 胡克
 * @since 2021-10-25 20:26:54 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoodsExcelVO {

  @ExcelProperty("商品分类")
  private String categoryName;

  @ExcelProperty("商品名称")
  private String goodsName;

  @ExcelProperty("商品状态错误")
  private String goodsStatus;

  @ExcelProperty("产地")
  private String place;

  @ExcelProperty("商品价格")
  private BigDecimal price;

  @ExcelProperty("备注")
  private String remark;
}
