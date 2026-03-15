package net.lab1024.sa.igaming.activity.turnover.manager;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Option;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.common.core.tenant.TenantContext;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverRiskActionRuleDao;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverRuleChangeLogDao;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverRiskActionRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverRuleChangeLogEntity;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turnover risk action rule manager — transaction + cache management for risk action rules.
 *
 * <p>{@code @Transactional} / {@code @Cacheable} / {@code @CacheEvict} must reside in Manager layer
 * per SmartAdmin rules.
 *
 * @author iGaming Team
 * @since 2026-03-12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TurnoverRiskActionRuleManager {

  private final TurnoverRiskActionRuleDao riskActionRuleDao;
  private final TurnoverRuleChangeLogDao changeLogDao;
  private final ObjectMapper objectMapper;

  @Cacheable(
      value = "turnover:risk-action",
      key = "'tenant:' + #tenantId + ':riskLevel:' + #riskLevel")
  public Option<TurnoverRiskActionRuleEntity> getActiveRule(Long tenantId, Integer riskLevel) {
    TurnoverRiskActionRuleEntity entity =
        riskActionRuleDao.selectOne(
            Wrappers.<TurnoverRiskActionRuleEntity>lambdaQuery()
                .eq(TurnoverRiskActionRuleEntity::getRiskLevel, riskLevel)
                .eq(TurnoverRiskActionRuleEntity::getStatus, 1)
                .eq(TurnoverRiskActionRuleEntity::getDeleted, false)
                .le(
                    TurnoverRiskActionRuleEntity::getEffectiveFrom,
                    OffsetDateTime.now(ZoneOffset.UTC))
                .ge(
                    TurnoverRiskActionRuleEntity::getEffectiveTo,
                    OffsetDateTime.now(ZoneOffset.UTC))
                .last("LIMIT 1"));
    return Option.of(entity);
  }

  @Transactional(rollbackFor = Throwable.class)
  @CacheEvict(value = "turnover:risk-action", allEntries = true)
  public void addRule(
      TurnoverRiskActionRuleEntity entity,
      Long operatorId,
      String operatorName,
      String changeReason) {
    entity.setTenantId(TenantContext.getTenantId());
    entity.setDeleted(false);
    riskActionRuleDao.insert(entity);
    recordChangeLog(4, entity, null, 1, operatorId, operatorName, changeReason);
    log.info("Added risk action rule: ruleId={}, operator={}", entity.getRuleId(), operatorName);
  }

  @Transactional(rollbackFor = Throwable.class)
  @CacheEvict(value = "turnover:risk-action", allEntries = true)
  public void updateRule(
      TurnoverRiskActionRuleEntity entity,
      Long operatorId,
      String operatorName,
      String changeReason) {
    TurnoverRiskActionRuleEntity oldEntity = riskActionRuleDao.selectById(entity.getRuleId());
    if (oldEntity == null || oldEntity.getDeleted()) {
      throw new IllegalArgumentException("Rule not found or already deleted");
    }
    int rows = riskActionRuleDao.updateById(entity);
    if (rows == 0) {
      throw new org.springframework.dao.OptimisticLockingFailureException("Version conflict");
    }
    recordChangeLog(4, entity, oldEntity, 2, operatorId, operatorName, changeReason);
    log.info("Updated risk action rule: ruleId={}, operator={}", entity.getRuleId(), operatorName);
  }

  @Transactional(rollbackFor = Throwable.class)
  @CacheEvict(value = "turnover:risk-action", allEntries = true)
  public void deleteRule(Long ruleId, Long operatorId, String operatorName, String changeReason) {
    TurnoverRiskActionRuleEntity oldEntity = riskActionRuleDao.selectById(ruleId);
    if (oldEntity == null || oldEntity.getDeleted()) {
      throw new IllegalArgumentException("Rule not found or already deleted");
    }
    oldEntity.setDeleted(true);
    riskActionRuleDao.updateById(oldEntity);
    recordChangeLog(4, null, oldEntity, 3, operatorId, operatorName, changeReason);
    log.info("Deleted risk action rule: ruleId={}, operator={}", ruleId, operatorName);
  }

  @CacheEvict(value = "turnover:risk-action", allEntries = true)
  public void evictCache(Long tenantId) {
    log.info("Evicted risk action rule cache for tenant: {}", tenantId);
  }

  private void recordChangeLog(
      Integer ruleType,
      TurnoverRiskActionRuleEntity newEntity,
      TurnoverRiskActionRuleEntity oldEntity,
      Integer operationType,
      Long operatorId,
      String operatorName,
      String changeReason) {
    TurnoverRuleChangeLogEntity changeLog = new TurnoverRuleChangeLogEntity();
    changeLog.setTenantId(TenantContext.getTenantId());
    changeLog.setRuleType(ruleType);
    changeLog.setRuleId(newEntity != null ? newEntity.getRuleId() : oldEntity.getRuleId());
    changeLog.setRuleCode(newEntity != null ? newEntity.getRuleCode() : oldEntity.getRuleCode());
    changeLog.setOperationType(operationType);
    changeLog.setOldValue(oldEntity != null ? entityToJson(oldEntity) : null);
    changeLog.setNewValue(newEntity != null ? entityToJson(newEntity) : null);
    changeLog.setChangeReason(changeReason);
    changeLog.setOperatorId(operatorId);
    changeLog.setOperatorName(operatorName);
    changeLog.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));
    changeLogDao.insert(changeLog);
  }

  private String entityToJson(TurnoverRiskActionRuleEntity entity) {
    try {
      return objectMapper.writeValueAsString(entity);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize entity to JSON", e);
      return "{}";
    }
  }
}
