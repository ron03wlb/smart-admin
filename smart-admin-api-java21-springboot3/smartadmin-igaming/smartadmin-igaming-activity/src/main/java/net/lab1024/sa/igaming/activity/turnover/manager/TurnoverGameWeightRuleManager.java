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
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverGameWeightRuleDao;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverRuleChangeLogDao;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverGameWeightRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverRuleChangeLogEntity;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Turnover game weight rule manager — transaction + cache management for game weight rules.
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
public class TurnoverGameWeightRuleManager {

  private final TurnoverGameWeightRuleDao gameWeightRuleDao;
  private final TurnoverRuleChangeLogDao changeLogDao;
  private final ObjectMapper objectMapper;

  /**
   * Get active rule for a specific game category (cached).
   *
   * @param tenantId tenant ID
   * @param gameCategory game category (1-6)
   * @return active rule entity, or Option.none() if not found
   */
  @Cacheable(
      value = "turnover:game-weight",
      key = "'tenant:' + #tenantId + ':category:' + #gameCategory")
  public Option<TurnoverGameWeightRuleEntity> getActiveRule(Long tenantId, Integer gameCategory) {
    TurnoverGameWeightRuleEntity entity =
        gameWeightRuleDao.selectOne(
            Wrappers.<TurnoverGameWeightRuleEntity>lambdaQuery()
                .eq(TurnoverGameWeightRuleEntity::getGameCategory, gameCategory)
                .eq(TurnoverGameWeightRuleEntity::getStatus, 1) // ENABLED
                .eq(TurnoverGameWeightRuleEntity::getDeleted, false)
                .le(
                    TurnoverGameWeightRuleEntity::getEffectiveFrom,
                    OffsetDateTime.now(ZoneOffset.UTC))
                .ge(
                    TurnoverGameWeightRuleEntity::getEffectiveTo,
                    OffsetDateTime.now(ZoneOffset.UTC))
                .orderByDesc(TurnoverGameWeightRuleEntity::getPriority)
                .last("LIMIT 1"));
    return Option.of(entity);
  }

  /**
   * Get game weight percentage for a specific game category.
   *
   * <p>Uses {@link TurnoverGameWeightRuleDao#selectGameWeight(Long, Integer)} with caching.
   *
   * @param tenantId tenant ID
   * @param gameCategory game category (1-6)
   * @return weight percentage (0.00 ~ 100.00), or null if no active rule found
   */
  @Cacheable(
      value = "turnover:game-weight",
      key = "'tenant:' + #tenantId + ':category:' + #gameCategory + ':weight'")
  public java.math.BigDecimal getGameWeight(Long tenantId, Integer gameCategory) {
    return gameWeightRuleDao.selectGameWeight(tenantId, gameCategory);
  }

  /**
   * Add a new game weight rule (transactional + cache eviction + change log).
   *
   * @param entity rule entity to add
   * @param operatorId employee ID who performed the operation
   * @param operatorName employee name
   * @param changeReason reason for adding the rule
   * @throws org.springframework.dao.DuplicateKeyException if rule_code already exists for tenant
   */
  @Transactional(rollbackFor = Throwable.class)
  @CacheEvict(value = "turnover:game-weight", allEntries = true)
  public void addRule(
      TurnoverGameWeightRuleEntity entity,
      Long operatorId,
      String operatorName,
      String changeReason) {

    entity.setTenantId(TenantContext.getTenantId());
    entity.setDeleted(false);

    gameWeightRuleDao.insert(entity);

    // Record change log
    recordChangeLog(
        1, // ruleType: GameWeight
        entity.getRuleId(),
        entity.getRuleCode(),
        1, // operationType: INSERT
        null, // oldValue
        entityToJson(entity), // newValue
        changeReason,
        operatorId,
        operatorName);

    log.info(
        "Added game weight rule: ruleId={}, ruleCode={}, operator={}",
        entity.getRuleId(),
        entity.getRuleCode(),
        operatorName);
  }

  /**
   * Update an existing game weight rule (transactional + cache eviction + change log).
   *
   * @param entity rule entity with updated values (must include ruleId + version for optimistic
   *     lock)
   * @param operatorId employee ID who performed the operation
   * @param operatorName employee name
   * @param changeReason reason for updating the rule
   * @throws org.springframework.dao.OptimisticLockingFailureException if version conflict
   */
  @Transactional(rollbackFor = Throwable.class)
  @CacheEvict(value = "turnover:game-weight", allEntries = true)
  public void updateRule(
      TurnoverGameWeightRuleEntity entity,
      Long operatorId,
      String operatorName,
      String changeReason) {

    // Read old value for change log
    TurnoverGameWeightRuleEntity oldEntity = gameWeightRuleDao.selectById(entity.getRuleId());
    if (oldEntity == null || oldEntity.getDeleted()) {
      throw new IllegalArgumentException("Rule not found or already deleted");
    }

    // Update
    int rows = gameWeightRuleDao.updateById(entity);
    if (rows == 0) {
      throw new org.springframework.dao.OptimisticLockingFailureException(
          "Version conflict: rule was modified by another user");
    }

    // Record change log
    recordChangeLog(
        1, // ruleType: GameWeight
        entity.getRuleId(),
        entity.getRuleCode(),
        2, // operationType: UPDATE
        entityToJson(oldEntity), // oldValue
        entityToJson(entity), // newValue
        changeReason,
        operatorId,
        operatorName);

    log.info(
        "Updated game weight rule: ruleId={}, ruleCode={}, operator={}",
        entity.getRuleId(),
        entity.getRuleCode(),
        operatorName);
  }

  /**
   * Delete a game weight rule (soft delete + cache eviction + change log).
   *
   * @param ruleId rule ID to delete
   * @param operatorId employee ID who performed the operation
   * @param operatorName employee name
   * @param changeReason reason for deleting the rule
   */
  @Transactional(rollbackFor = Throwable.class)
  @CacheEvict(value = "turnover:game-weight", allEntries = true)
  public void deleteRule(Long ruleId, Long operatorId, String operatorName, String changeReason) {

    // Read old value for change log
    TurnoverGameWeightRuleEntity oldEntity = gameWeightRuleDao.selectById(ruleId);
    if (oldEntity == null || oldEntity.getDeleted()) {
      throw new IllegalArgumentException("Rule not found or already deleted");
    }

    // Soft delete
    oldEntity.setDeleted(true);
    gameWeightRuleDao.updateById(oldEntity);

    // Record change log
    recordChangeLog(
        1, // ruleType: GameWeight
        ruleId,
        oldEntity.getRuleCode(),
        3, // operationType: DELETE
        entityToJson(oldEntity), // oldValue
        null, // newValue
        changeReason,
        operatorId,
        operatorName);

    log.info("Deleted game weight rule: ruleId={}, operator={}", ruleId, operatorName);
  }

  /**
   * Evict all cache entries for game weight rules.
   *
   * @param tenantId tenant ID (for logging purposes)
   */
  @CacheEvict(value = "turnover:game-weight", allEntries = true)
  public void evictCache(Long tenantId) {
    log.info("Evicted game weight rule cache for tenant: {}", tenantId);
  }

  // --- Private Helper Methods ---

  private void recordChangeLog(
      Integer ruleType,
      Long ruleId,
      String ruleCode,
      Integer operationType,
      String oldValue,
      String newValue,
      String changeReason,
      Long operatorId,
      String operatorName) {

    TurnoverRuleChangeLogEntity changeLog = new TurnoverRuleChangeLogEntity();
    changeLog.setTenantId(TenantContext.getTenantId());
    changeLog.setRuleType(ruleType);
    changeLog.setRuleId(ruleId);
    changeLog.setRuleCode(ruleCode);
    changeLog.setOperationType(operationType);
    changeLog.setOldValue(oldValue);
    changeLog.setNewValue(newValue);
    changeLog.setChangeReason(changeReason);
    changeLog.setOperatorId(operatorId);
    changeLog.setOperatorName(operatorName);
    changeLog.setCreateTime(OffsetDateTime.now(ZoneOffset.UTC));

    changeLogDao.insert(changeLog);
  }

  private String entityToJson(TurnoverGameWeightRuleEntity entity) {
    try {
      return objectMapper.writeValueAsString(entity);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize entity to JSON", e);
      return "{}";
    }
  }
}
