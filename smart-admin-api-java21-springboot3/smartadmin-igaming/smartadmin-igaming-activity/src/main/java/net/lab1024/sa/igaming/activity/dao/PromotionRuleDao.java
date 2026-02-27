package net.lab1024.sa.igaming.activity.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.igaming.activity.domain.entity.PromotionRuleEntity;
import net.lab1024.sa.igaming.activity.domain.form.PromotionRuleQueryForm;
import net.lab1024.sa.igaming.activity.domain.vo.PromotionRuleVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Promotion rule DAO.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Mapper
public interface PromotionRuleDao extends BaseMapper<PromotionRuleEntity> {

  List<PromotionRuleVO> queryPage(Page<?> page, @Param("query") PromotionRuleQueryForm queryForm);

  PromotionRuleEntity selectByCode(
      @Param("tenantId") Long tenantId, @Param("promotionCode") String promotionCode);

  List<PromotionRuleEntity> selectActiveRules(@Param("tenantId") Long tenantId);
}
