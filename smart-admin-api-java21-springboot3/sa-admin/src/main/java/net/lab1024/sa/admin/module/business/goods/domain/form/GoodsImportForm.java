package net.lab1024.sa.admin.module.business.goods.domain.form;

import cn.idev.excel.annotation.ExcelProperty;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 商品 导入表单
 *
 * @author 1024创新实验室: 胡克
 * @since 2021-10-25 20:26:54 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class GoodsImportForm {

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
