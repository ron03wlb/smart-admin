package net.lab1024.sa.igaming.activity.turnover.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Option;
import java.math.BigDecimal;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverGameWeightRuleDao;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverRuleChangeLogDao;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverGameWeightRuleEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverRuleChangeLogEntity;
import net.lab1024.sa.igaming.activity.turnover.service.TurnoverTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Unit tests for {@link TurnoverGameWeightRuleManager}.
 *
 * <p>Tests transaction management, cache operations, and change log recording.
 *
 * @author iGaming Team
 * @since 2026-03-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TurnoverGameWeightRuleManager 單元測試")
class TurnoverGameWeightRuleManagerTest {

  @Mock private TurnoverGameWeightRuleDao gameWeightRuleDao;
  @Mock private TurnoverRuleChangeLogDao changeLogDao;
  @Mock private ObjectMapper objectMapper;

  private TurnoverGameWeightRuleManager manager;

  @BeforeEach
  void setUp() {
    manager = new TurnoverGameWeightRuleManager(gameWeightRuleDao, changeLogDao, objectMapper);
  }

  // ===== getActiveRule() 測試 =====

  @Nested
  @DisplayName("getActiveRule() 測試")
  class GetActiveRuleTests {

    @Test
    @DisplayName("當規則存在時應該返回Option.some")
    void whenRuleExists_ShouldReturnSome() {
      // Arrange
      TurnoverGameWeightRuleEntity entity =
          TurnoverTestFixture.createGameWeightRule(1, new BigDecimal("100.00"));
      when(gameWeightRuleDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

      // Act
      Option<TurnoverGameWeightRuleEntity> result = manager.getActiveRule(1L, 1);

      // Assert
      assertThat(result.isDefined()).isTrue();
      assertThat(result.get().getGameCategory()).isEqualTo(1);
      assertThat(result.get().getWeightPercentage()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("當規則不存在時應該返回Option.none")
    void whenRuleNotFound_ShouldReturnNone() {
      // Arrange
      when(gameWeightRuleDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      // Act
      Option<TurnoverGameWeightRuleEntity> result = manager.getActiveRule(1L, 999);

      // Assert
      assertThat(result.isEmpty()).isTrue();
    }
  }

  // ===== getGameWeight() 測試 =====

  @Nested
  @DisplayName("getGameWeight() 測試")
  class GetGameWeightTests {

    @Test
    @DisplayName("當權重存在時應該返回正確的BigDecimal")
    void whenWeightExists_ShouldReturnCorrectValue() {
      // Arrange
      when(gameWeightRuleDao.selectGameWeight(1L, 1)).thenReturn(new BigDecimal("100.00"));

      // Act
      BigDecimal result = manager.getGameWeight(1L, 1);

      // Assert
      assertThat(result).isEqualByComparingTo("100.00");
      verify(gameWeightRuleDao, times(1)).selectGameWeight(1L, 1);
    }

    @Test
    @DisplayName("當權重不存在時應該返回null")
    void whenWeightNotFound_ShouldReturnNull() {
      // Arrange
      when(gameWeightRuleDao.selectGameWeight(1L, 999)).thenReturn(null);

      // Act
      BigDecimal result = manager.getGameWeight(1L, 999);

      // Assert
      assertThat(result).isNull();
    }
  }

  // ===== addRule() 測試 =====

  @Nested
  @DisplayName("addRule() 測試")
  class AddRuleTests {

    @Test
    @DisplayName("應該成功新增規則並記錄變更日誌")
    void shouldAddRuleSuccessfully() throws JsonProcessingException {
      // Arrange
      TurnoverGameWeightRuleEntity entity =
          TurnoverTestFixture.createGameWeightRule(1, new BigDecimal("100.00"));
      when(gameWeightRuleDao.insert(any(TurnoverGameWeightRuleEntity.class))).thenReturn(1);
      when(objectMapper.writeValueAsString(any())).thenReturn("{\"ruleId\":1}");

      // Act
      manager.addRule(entity, 1001L, "Admin", "Initial setup");

      // Assert
      verify(gameWeightRuleDao, times(1)).insert(any(TurnoverGameWeightRuleEntity.class));
      assertThat(entity.getDeleted()).isFalse();

      // Verify change log was recorded
      ArgumentCaptor<TurnoverRuleChangeLogEntity> captor =
          ArgumentCaptor.forClass(TurnoverRuleChangeLogEntity.class);
      verify(changeLogDao, times(1)).insert(captor.capture());

      TurnoverRuleChangeLogEntity changeLog = captor.getValue();
      assertThat(changeLog.getRuleType()).isEqualTo(1); // GameWeight
      assertThat(changeLog.getOperationType()).isEqualTo(1); // INSERT
      assertThat(changeLog.getOldValue()).isNull();
      assertThat(changeLog.getNewValue()).isEqualTo("{\"ruleId\":1}");
      assertThat(changeLog.getChangeReason()).isEqualTo("Initial setup");
      assertThat(changeLog.getOperatorId()).isEqualTo(1001L);
      assertThat(changeLog.getOperatorName()).isEqualTo("Admin");
    }

    @Test
    @DisplayName("當JSON序列化失敗時應該使用空對象")
    void whenJsonSerializationFails_ShouldUseEmptyObject() throws JsonProcessingException {
      // Arrange
      TurnoverGameWeightRuleEntity entity =
          TurnoverTestFixture.createGameWeightRule(1, new BigDecimal("100.00"));
      when(gameWeightRuleDao.insert(any(TurnoverGameWeightRuleEntity.class))).thenReturn(1);
      when(objectMapper.writeValueAsString(any()))
          .thenThrow(new JsonProcessingException("Serialization error") {});

      // Act
      manager.addRule(entity, 1001L, "Admin", "Initial setup");

      // Assert - Should not throw exception
      verify(gameWeightRuleDao, times(1)).insert(any(TurnoverGameWeightRuleEntity.class));

      // Verify change log was still recorded with empty JSON
      ArgumentCaptor<TurnoverRuleChangeLogEntity> captor =
          ArgumentCaptor.forClass(TurnoverRuleChangeLogEntity.class);
      verify(changeLogDao, times(1)).insert(captor.capture());
      assertThat(captor.getValue().getNewValue()).isEqualTo("{}");
    }
  }

  // ===== updateRule() 測試 =====

  @Nested
  @DisplayName("updateRule() 測試")
  class UpdateRuleTests {

    @Test
    @DisplayName("應該成功更新規則並記錄變更日誌")
    void shouldUpdateRuleSuccessfully() throws JsonProcessingException {
      // Arrange
      TurnoverGameWeightRuleEntity oldEntity =
          TurnoverTestFixture.createGameWeightRule(1, new BigDecimal("100.00"));
      oldEntity.setRuleId(1L);
      oldEntity.setDeleted(false);

      TurnoverGameWeightRuleEntity newEntity =
          TurnoverTestFixture.createGameWeightRule(1, new BigDecimal("80.00"));
      newEntity.setRuleId(1L);

      when(gameWeightRuleDao.selectById(1L)).thenReturn(oldEntity);
      when(gameWeightRuleDao.updateById(any(TurnoverGameWeightRuleEntity.class))).thenReturn(1);
      when(objectMapper.writeValueAsString(oldEntity)).thenReturn("{\"weight\":100}");
      when(objectMapper.writeValueAsString(newEntity)).thenReturn("{\"weight\":80}");

      // Act
      manager.updateRule(newEntity, 1001L, "Admin", "Weight adjustment");

      // Assert
      verify(gameWeightRuleDao, times(1)).updateById(any(TurnoverGameWeightRuleEntity.class));

      // Verify change log
      ArgumentCaptor<TurnoverRuleChangeLogEntity> captor =
          ArgumentCaptor.forClass(TurnoverRuleChangeLogEntity.class);
      verify(changeLogDao, times(1)).insert(captor.capture());

      TurnoverRuleChangeLogEntity changeLog = captor.getValue();
      assertThat(changeLog.getOperationType()).isEqualTo(2); // UPDATE
      assertThat(changeLog.getOldValue()).isEqualTo("{\"weight\":100}");
      assertThat(changeLog.getNewValue()).isEqualTo("{\"weight\":80}");
      assertThat(changeLog.getChangeReason()).isEqualTo("Weight adjustment");
    }

    @Test
    @DisplayName("當規則不存在時應該拋出異常")
    void whenRuleNotFound_ShouldThrowException() {
      // Arrange
      TurnoverGameWeightRuleEntity entity =
          TurnoverTestFixture.createGameWeightRule(1, new BigDecimal("100.00"));
      entity.setRuleId(999L);
      when(gameWeightRuleDao.selectById(999L)).thenReturn(null);

      // Act & Assert
      assertThatThrownBy(() -> manager.updateRule(entity, 1001L, "Admin", "Update"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Rule not found");

      verify(gameWeightRuleDao, never()).updateById(any(TurnoverGameWeightRuleEntity.class));
      verify(changeLogDao, never()).insert(any(TurnoverRuleChangeLogEntity.class));
    }

    @Test
    @DisplayName("當規則已刪除時應該拋出異常")
    void whenRuleAlreadyDeleted_ShouldThrowException() {
      // Arrange
      TurnoverGameWeightRuleEntity oldEntity =
          TurnoverTestFixture.createGameWeightRule(1, new BigDecimal("100.00"));
      oldEntity.setRuleId(1L);
      oldEntity.setDeleted(true); // Already deleted

      TurnoverGameWeightRuleEntity newEntity =
          TurnoverTestFixture.createGameWeightRule(1, new BigDecimal("80.00"));
      newEntity.setRuleId(1L);

      when(gameWeightRuleDao.selectById(1L)).thenReturn(oldEntity);

      // Act & Assert
      assertThatThrownBy(() -> manager.updateRule(newEntity, 1001L, "Admin", "Update"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("already deleted");

      verify(gameWeightRuleDao, never()).updateById(any(TurnoverGameWeightRuleEntity.class));
    }

    @Test
    @DisplayName("當版本衝突時應該拋出樂觀鎖異常")
    void whenVersionConflict_ShouldThrowOptimisticLockException() {
      // Arrange
      TurnoverGameWeightRuleEntity oldEntity =
          TurnoverTestFixture.createGameWeightRule(1, new BigDecimal("100.00"));
      oldEntity.setRuleId(1L);
      oldEntity.setDeleted(false);

      TurnoverGameWeightRuleEntity newEntity =
          TurnoverTestFixture.createGameWeightRule(1, new BigDecimal("80.00"));
      newEntity.setRuleId(1L);

      when(gameWeightRuleDao.selectById(1L)).thenReturn(oldEntity);
      when(gameWeightRuleDao.updateById(any(TurnoverGameWeightRuleEntity.class)))
          .thenReturn(0); // 0 rows updated = version conflict

      // Act & Assert
      assertThatThrownBy(() -> manager.updateRule(newEntity, 1001L, "Admin", "Update"))
          .isInstanceOf(OptimisticLockingFailureException.class)
          .hasMessageContaining("Version conflict");

      verify(changeLogDao, never()).insert(any(TurnoverRuleChangeLogEntity.class));
    }
  }

  // ===== deleteRule() 測試 =====

  @Nested
  @DisplayName("deleteRule() 測試")
  class DeleteRuleTests {

    @Test
    @DisplayName("應該成功軟刪除規則並記錄變更日誌")
    void shouldDeleteRuleSuccessfully() throws JsonProcessingException {
      // Arrange
      TurnoverGameWeightRuleEntity entity =
          TurnoverTestFixture.createGameWeightRule(1, new BigDecimal("100.00"));
      entity.setRuleId(1L);
      entity.setDeleted(false);

      when(gameWeightRuleDao.selectById(1L)).thenReturn(entity);
      when(gameWeightRuleDao.updateById(any(TurnoverGameWeightRuleEntity.class))).thenReturn(1);
      when(objectMapper.writeValueAsString(any())).thenReturn("{\"ruleId\":1}");

      // Act
      manager.deleteRule(1L, 1001L, "Admin", "No longer needed");

      // Assert
      verify(gameWeightRuleDao, times(1)).updateById(any(TurnoverGameWeightRuleEntity.class));
      assertThat(entity.getDeleted()).isTrue();

      // Verify change log
      ArgumentCaptor<TurnoverRuleChangeLogEntity> captor =
          ArgumentCaptor.forClass(TurnoverRuleChangeLogEntity.class);
      verify(changeLogDao, times(1)).insert(captor.capture());

      TurnoverRuleChangeLogEntity changeLog = captor.getValue();
      assertThat(changeLog.getOperationType()).isEqualTo(3); // DELETE
      assertThat(changeLog.getOldValue()).isEqualTo("{\"ruleId\":1}");
      assertThat(changeLog.getNewValue()).isNull();
      assertThat(changeLog.getChangeReason()).isEqualTo("No longer needed");
    }

    @Test
    @DisplayName("當規則不存在時應該拋出異常")
    void whenRuleNotFound_ShouldThrowException() {
      // Arrange
      when(gameWeightRuleDao.selectById(999L)).thenReturn(null);

      // Act & Assert
      assertThatThrownBy(() -> manager.deleteRule(999L, 1001L, "Admin", "Delete"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Rule not found");

      verify(gameWeightRuleDao, never()).updateById(any(TurnoverGameWeightRuleEntity.class));
      verify(changeLogDao, never()).insert(any(TurnoverRuleChangeLogEntity.class));
    }

    @Test
    @DisplayName("當規則已刪除時應該拋出異常")
    void whenRuleAlreadyDeleted_ShouldThrowException() {
      // Arrange
      TurnoverGameWeightRuleEntity entity =
          TurnoverTestFixture.createGameWeightRule(1, new BigDecimal("100.00"));
      entity.setRuleId(1L);
      entity.setDeleted(true); // Already deleted

      when(gameWeightRuleDao.selectById(1L)).thenReturn(entity);

      // Act & Assert
      assertThatThrownBy(() -> manager.deleteRule(1L, 1001L, "Admin", "Delete"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("already deleted");

      verify(gameWeightRuleDao, never()).updateById(any(TurnoverGameWeightRuleEntity.class));
    }
  }

  // ===== evictCache() 測試 =====

  @Test
  @DisplayName("evictCache 應該執行無異常")
  void evictCacheShouldExecuteWithoutException() {
    // Act & Assert - Should not throw
    manager.evictCache(1L);
  }
}
