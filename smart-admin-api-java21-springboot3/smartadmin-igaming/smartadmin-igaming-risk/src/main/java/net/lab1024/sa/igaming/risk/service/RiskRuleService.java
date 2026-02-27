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
import net.lab1024.sa.igaming.risk.dao.RiskRuleParamDao;
import net.lab1024.sa.igaming.risk.domain.entity.RiskRuleParamEntity;
import net.lab1024.sa.igaming.risk.domain.form.RiskRuleAddForm;
import net.lab1024.sa.igaming.risk.domain.form.RiskRuleQueryForm;
import net.lab1024.sa.igaming.risk.domain.form.RiskRuleUpdateForm;
import net.lab1024.sa.igaming.risk.domain.vo.RiskRuleVO;
import org.springframework.stereotype.Service;

/**
 * Risk rule CRUD service.
 *
 * @author iGaming Team
 * @since 2026-02-18
 */
@Service
@RequiredArgsConstructor
public class RiskRuleService {

  private final RiskRuleParamDao riskRuleParamDao;

  @SuppressWarnings("unchecked")
  public ResponseDTO<PageResult<RiskRuleVO>> queryPage(RiskRuleQueryForm form) {
    Page<RiskRuleParamEntity> page =
        (Page<RiskRuleParamEntity>) SmartPageUtil.convert2PageQuery(form);

    LambdaQueryWrapper<RiskRuleParamEntity> wrapper =
        Wrappers.<RiskRuleParamEntity>lambdaQuery()
            .eq(RiskRuleParamEntity::getDeleted, false)
            .eq(form.getRuleType() != null, RiskRuleParamEntity::getRuleType, form.getRuleType())
            .eq(form.getEnabled() != null, RiskRuleParamEntity::getEnabled, form.getEnabled())
            .like(
                form.getKeyword() != null && !form.getKeyword().isBlank(),
                RiskRuleParamEntity::getRuleName,
                form.getKeyword())
            .orderByDesc(RiskRuleParamEntity::getCreateTime);

    Page<RiskRuleParamEntity> result = riskRuleParamDao.selectPage(page, wrapper);
    PageResult<RiskRuleVO> pageResult =
        SmartPageUtil.convert2PageResult(page, result.getRecords(), RiskRuleVO.class);
    return ResponseDTO.ok(pageResult);
  }

  public ResponseDTO<RiskRuleVO> getById(Long ruleParamId) {
    return Option.of(riskRuleParamDao.selectById(ruleParamId))
        .filter(e -> !e.getDeleted())
        .map(e -> SmartBeanUtil.copy(e, RiskRuleVO.class))
        .map(ResponseDTO::ok)
        .getOrElse(() -> ResponseDTO.userErrorParam("Risk rule does not exist"));
  }

  public ResponseDTO<String> add(RiskRuleAddForm form) {
    RiskRuleParamEntity entity = SmartBeanUtil.copy(form, RiskRuleParamEntity.class);
    entity.setEnabled(true);
    entity.setDeleted(false);
    riskRuleParamDao.insert(entity);
    return ResponseDTO.ok();
  }

  public ResponseDTO<String> update(RiskRuleUpdateForm form) {
    return Option.of(riskRuleParamDao.selectById(form.getRuleParamId()))
        .filter(e -> !e.getDeleted())
        .map(
            entity -> {
              if (form.getRuleName() != null) {
                entity.setRuleName(form.getRuleName());
              }
              if (form.getRuleDescription() != null) {
                entity.setRuleDescription(form.getRuleDescription());
              }
              if (form.getThresholdValue() != null) {
                entity.setThresholdValue(form.getThresholdValue());
              }
              if (form.getTimeWindowSeconds() != null) {
                entity.setTimeWindowSeconds(form.getTimeWindowSeconds());
              }
              if (form.getMaxCount() != null) {
                entity.setMaxCount(form.getMaxCount());
              }
              if (form.getWeight() != null) {
                entity.setWeight(form.getWeight());
              }
              if (form.getEnabled() != null) {
                entity.setEnabled(form.getEnabled());
              }
              if (form.getParamsJson() != null) {
                entity.setParamsJson(form.getParamsJson());
              }
              riskRuleParamDao.updateById(entity);
              return ResponseDTO.<String>ok();
            })
        .getOrElse(() -> ResponseDTO.userErrorParam("Risk rule does not exist"));
  }

  public ResponseDTO<String> delete(Long ruleParamId) {
    return Option.of(riskRuleParamDao.selectById(ruleParamId))
        .filter(e -> !e.getDeleted())
        .map(
            entity -> {
              entity.setDeleted(true);
              riskRuleParamDao.updateById(entity);
              return ResponseDTO.<String>ok();
            })
        .getOrElse(() -> ResponseDTO.userErrorParam("Risk rule does not exist"));
  }
}
