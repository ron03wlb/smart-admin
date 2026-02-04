package net.lab1024.sa.business.goods.domain.form;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.lab1024.sa.business.goods.constant.GoodsStatusEnum;
import net.lab1024.sa.common.core.domain.request.PageParam;
import net.lab1024.sa.common.json.deserializer.DictDataDeserializer;
import net.lab1024.sa.common.swagger.annotation.SchemaEnum;
import net.lab1024.sa.common.validation.annotation.CheckEnum;
import org.hibernate.validator.constraints.Length;

/**
 * 商品 分页查询
 *
 * @author 1024创新实验室: 胡克
 * @since 2021-10-25 20:26:54 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class GoodsQueryForm extends PageParam {

  @Schema(description = "商品分类")
  private Integer categoryId;

  @Schema(description = "搜索词")
  @Length(max = 30, message = "搜索词最多30字符")
  private String searchWord;

  @SchemaEnum(GoodsStatusEnum.class)
  @CheckEnum(message = "商品状态错误", value = GoodsStatusEnum.class, required = false)
  private Integer goodsStatus;

  @Schema(description = "产地")
  @JsonDeserialize(using = DictDataDeserializer.class)
  private String place;

  @Schema(description = "上架状态")
  private Boolean shelvesFlag;

  @Schema(hidden = true)
  private Boolean deletedFlag;
}
