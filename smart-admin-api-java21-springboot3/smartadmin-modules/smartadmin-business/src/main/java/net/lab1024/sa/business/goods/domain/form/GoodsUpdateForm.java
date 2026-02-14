package net.lab1024.sa.business.goods.domain.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品 更新表单
 *
 * @author 1024创新实验室: 胡克
 * @since 2021-10-25 20:26:54 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GoodsUpdateForm extends GoodsAddForm {

  @Schema(description = "商品id")
  @NotNull(message = "商品id不能为空")
  private Long goodsId;
}
