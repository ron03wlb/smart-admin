package net.lab1024.sa.igaming.risk.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import net.lab1024.sa.common.mybatis.util.SmartPageUtil;
import net.lab1024.sa.igaming.risk.dao.RiskScoreDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskScoreEntity;
import net.lab1024.sa.igaming.risk.domain.form.RiskScoreQueryForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskScoreVO;
import org.springframework.stereotype.Service;

/**
 * Risk score query service.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class RiskScoreService {

  private final RiskScoreDao riskScoreDao;

  @SuppressWarnings("unchecked")
  public ResponseDTO<PageResult<RiskScoreVO>> queryPage(RiskScoreQueryForm form) {
    Page<RiskScoreEntity> page = (Page<RiskScoreEntity>) SmartPageUtil.convert2PageQuery(form);

    LambdaQueryWrapper<RiskScoreEntity> wrapper =
        Wrappers.<RiskScoreEntity>lambdaQuery()
            .eq(form.getRiskLevel() != null, RiskScoreEntity::getRiskLevel, form.getRiskLevel())
            .eq(form.getAutoLocked() != null, RiskScoreEntity::getAutoLocked, form.getAutoLocked())
            .eq(form.getPlayerId() != null, RiskScoreEntity::getPlayerId, form.getPlayerId())
            .orderByDesc(RiskScoreEntity::getCumulativeScore);

    Page<RiskScoreEntity> result = riskScoreDao.selectPage(page, wrapper);
    PageResult<RiskScoreVO> pageResult =
        SmartPageUtil.convert2PageResult(page, result.getRecords(), RiskScoreVO.class);
    return ResponseDTO.ok(pageResult);
  }

  public ResponseDTO<RiskScoreVO> getByPlayerId(Long playerId, Long tenantId) {
    return Option.of(riskScoreDao.findByPlayerIdAndTenantId(playerId, tenantId))
        .map(e -> SmartBeanUtil.copy(e, RiskScoreVO.class))
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam("Risk score profile does not exist"));
  }
}
