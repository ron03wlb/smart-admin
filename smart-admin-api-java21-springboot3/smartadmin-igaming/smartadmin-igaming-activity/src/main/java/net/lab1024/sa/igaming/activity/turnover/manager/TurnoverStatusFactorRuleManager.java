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
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverRuleChangeLogDao;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverStatusFactorRuleDao;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverRuleChangeLogEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverStatusFactorRuleEntity;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turnover status factor rule manager — transaction + cache management for status factor rules.
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
public class TurnoverStatusFactorRuleManager {

  private final TurnoverStatusFactorRuleDao statusFactorRuleDao;
  private final TurnoverRuleChangeLogDao changeLogDao;
  private final ObjectMapper objectMapper;

  @Cacheable(
      value = "turnover:status-factor",
      key = "'tenant:' + #tenantId + ':status:' + #settlementStatus")
  public Option<TurnoverStatusFactorRuleEntity> getActiveRule(
      Long tenantId, Integer settlementStatus) {
    TurnoverStatusFactorRuleEntity entity =
        statusFactorRuleDao.selectOne(
            Wrappers.<TurnoverStatusFactorRuleEntity>lambdaQuery()
                .eq(TurnoverStatusFactorRuleEntity::getSettlementStatus, settlementStatus)
                .eq(TurnoverStatusFactorRuleEntity::getStatus, 1)
                .eq(TurnoverStatusFactorRuleEntity::getDeleted, false)
                .le(
                    TurnoverStatusFactorRuleEntity::getEffectiveFrom,
                    OffsetDateTime.now(ZoneOffset.UTC))
                .ge(
                    TurnoverStatusFactorRuleEntity::getEffectiveTo,
                    OffsetDateTime.now(ZoneOffset.UTC))
                .last("LIMIT 1"));
    return Option.of(entity);
  }

  /**
   * Get status factor percentage for a specific settlement status.
   *
   * <p>Uses {@link TurnoverStatusFactorRuleDao#selectStatusFactor(Long, Integer)} with caching.
   *
   * @param tenantId tenant ID
   * @param settlementStatus settlement status (1-9)
   * @return factor percentage (0.00 ~ 100.00), or null if no active rule found
   */
  @Cacheable(
      value = "turnover:status-factor",
      key = "'tenant:' + #tenantId + ':status:' + #settlementStatus + ':factor'")
  public java.math.BigDecimal getStatusFactor(Long tenantId, Integer settlementStatus) {
    return statusFactorRuleDao.selectStatusFactor(tenantId, settlementStatus);
  }

  @Transactional(rollbackFor = Throwable.class)
  @CacheEvict(value = "turnover:status-factor", allEntries = true)
  public void addRule(
      TurnoverStatusFactorRuleEntity entity,
      Long operatorId,
      String operatorName,
      String changeReason) {
    entity.setTenantId(TenantContext.getTenantId());
    entity.setDeleted(false);
    statusFactorRuleDao.insert(entity);
    recordChangeLog(2, entity, null, 1, operatorId, operatorName, changeReason);
    log.info("Added status factor rule: ruleId={}, operator={}", entity.getRuleId(), operatorName);
  }

  @Transactional(rollbackFor = Throwable.class)
  @CacheEvict(value = "turnover:status-factor", allEntries = true)
  public void updateRule(
      TurnoverStatusFactorRuleEntity entity,
      Long operatorId,
      String operatorName,
      String changeReason) {
    TurnoverStatusFactorRuleEntity oldEntity = statusFactorRuleDao.selectById(entity.getRuleId());
    if (oldEntity == null || oldEntity.getDeleted()) {
      throw new IllegalArgumentException("Rule not found or already deleted");
    }
    int rows = statusFactorRuleDao.updateById(entity);
    if (rows == 0) {
      throw new org.springframework.dao.OptimisticLockingFailureException("Version conflict");
    }
    recordChangeLog(2, entity, oldEntity, 2, operatorId, operatorName, changeReason);
    log.info(
        "Updated status factor rule: ruleId={}, operator={}", entity.getRuleId(), operatorName);
  }

  @Transactional(rollbackFor = Throwable.class)
  @CacheEvict(value = "turnover:status-factor", allEntries = true)
  public void deleteRule(Long ruleId, Long operatorId, String operatorName, String changeReason) {
    TurnoverStatusFactorRuleEntity oldEntity = statusFactorRuleDao.selectById(ruleId);
    if (oldEntity == null || oldEntity.getDeleted()) {
      throw new IllegalArgumentException("Rule not found or already deleted");
    }
    oldEntity.setDeleted(true);
    statusFactorRuleDao.updateById(oldEntity);
    recordChangeLog(2, null, oldEntity, 3, operatorId, operatorName, changeReason);
    log.info("Deleted status factor rule: ruleId={}, operator={}", ruleId, operatorName);
  }

  @CacheEvict(value = "turnover:status-factor", allEntries = true)
  public void evictCache(Long tenantId) {
    log.info("Evicted status factor rule cache for tenant: {}", tenantId);
  }

  private void recordChangeLog(
      Integer ruleType,
      TurnoverStatusFactorRuleEntity newEntity,
      TurnoverStatusFactorRuleEntity oldEntity,
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

  private String entityToJson(TurnoverStatusFactorRuleEntity entity) {
    try {
      return objectMapper.writeValueAsString(entity);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize entity to JSON", e);
      return "{}";
    }
  }
}
