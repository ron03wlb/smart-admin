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
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverRuleChangeLogDao;
import net.lab1024.sa.igaming.activity.turnover.dao.TurnoverStatusFactorRuleDao;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverRuleChangeLogEntity;
import net.lab1024.sa.igaming.activity.turnover.domain.entity.TurnoverStatusFactorRuleEntity;
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
 * Unit tests for {@link TurnoverStatusFactorRuleManager}.
 *
 * @author iGaming Team
 * @since 2026-03-15
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TurnoverStatusFactorRuleManager 單元測試")
class TurnoverStatusFactorRuleManagerTest {

  @Mock private TurnoverStatusFactorRuleDao statusFactorRuleDao;
  @Mock private TurnoverRuleChangeLogDao changeLogDao;
  @Mock private ObjectMapper objectMapper;

  private TurnoverStatusFactorRuleManager manager;

  @BeforeEach
  void setUp() {
    manager = new TurnoverStatusFactorRuleManager(statusFactorRuleDao, changeLogDao, objectMapper);
  }

  // ===== getActiveRule() 測試 =====

  @Nested
  @DisplayName("getActiveRule() 測試")
  class GetActiveRuleTests {

    @Test
    @DisplayName("當規則存在時應該返回Option.some")
    void whenRuleExists_ShouldReturnSome() {
      // Arrange
      TurnoverStatusFactorRuleEntity entity =
          TurnoverTestFixture.createStatusFactorRule(1, new BigDecimal("100.00"));
      when(statusFactorRuleDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(entity);

      // Act
      Option<TurnoverStatusFactorRuleEntity> result = manager.getActiveRule(1L, 1);

      // Assert
      assertThat(result.isDefined()).isTrue();
      assertThat(result.get().getSettlementStatus()).isEqualTo(1);
      assertThat(result.get().getFactorPercentage()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("當規則不存在時應該返回Option.none")
    void whenRuleNotFound_ShouldReturnNone() {
      // Arrange
      when(statusFactorRuleDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

      // Act
      Option<TurnoverStatusFactorRuleEntity> result = manager.getActiveRule(1L, 999);

      // Assert
      assertThat(result.isEmpty()).isTrue();
    }
  }

  // ===== getStatusFactor() 測試 =====

  @Nested
  @DisplayName("getStatusFactor() 測試")
  class GetStatusFactorTests {

    @Test
    @DisplayName("當因子存在時應該返回正確的BigDecimal")
    void whenFactorExists_ShouldReturnCorrectValue() {
      // Arrange
      when(statusFactorRuleDao.selectStatusFactor(1L, 1)).thenReturn(new BigDecimal("100.00"));

      // Act
      BigDecimal result = manager.getStatusFactor(1L, 1);

      // Assert
      assertThat(result).isEqualByComparingTo("100.00");
      verify(statusFactorRuleDao, times(1)).selectStatusFactor(1L, 1);
    }

    @Test
    @DisplayName("當因子不存在時應該返回null")
    void whenFactorNotFound_ShouldReturnNull() {
      // Arrange
      when(statusFactorRuleDao.selectStatusFactor(1L, 999)).thenReturn(null);

      // Act
      BigDecimal result = manager.getStatusFactor(1L, 999);

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
      TurnoverStatusFactorRuleEntity entity =
          TurnoverTestFixture.createStatusFactorRule(1, new BigDecimal("100.00"));
      when(statusFactorRuleDao.insert(any(TurnoverStatusFactorRuleEntity.class))).thenReturn(1);
      when(objectMapper.writeValueAsString(any())).thenReturn("{\"ruleId\":1}");

      // Act
      manager.addRule(entity, 1001L, "Admin", "Initial setup");

      // Assert
      verify(statusFactorRuleDao, times(1)).insert(any(TurnoverStatusFactorRuleEntity.class));
      assertThat(entity.getDeleted()).isFalse();

      // Verify change log
      ArgumentCaptor<TurnoverRuleChangeLogEntity> captor =
          ArgumentCaptor.forClass(TurnoverRuleChangeLogEntity.class);
      verify(changeLogDao, times(1)).insert(captor.capture());

      TurnoverRuleChangeLogEntity changeLog = captor.getValue();
      assertThat(changeLog.getRuleType()).isEqualTo(2); // StatusFactor
      assertThat(changeLog.getOperationType()).isEqualTo(1); // INSERT
      assertThat(changeLog.getOldValue()).isNull();
      assertThat(changeLog.getNewValue()).isEqualTo("{\"ruleId\":1}");
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
      TurnoverStatusFactorRuleEntity oldEntity =
          TurnoverTestFixture.createStatusFactorRule(1, new BigDecimal("100.00"));
      oldEntity.setRuleId(1L);
      oldEntity.setDeleted(false);

      TurnoverStatusFactorRuleEntity newEntity =
          TurnoverTestFixture.createStatusFactorRule(1, new BigDecimal("80.00"));
      newEntity.setRuleId(1L);

      when(statusFactorRuleDao.selectById(1L)).thenReturn(oldEntity);
      when(statusFactorRuleDao.updateById(any(TurnoverStatusFactorRuleEntity.class))).thenReturn(1);
      when(objectMapper.writeValueAsString(oldEntity)).thenReturn("{\"factor\":100}");
      when(objectMapper.writeValueAsString(newEntity)).thenReturn("{\"factor\":80}");

      // Act
      manager.updateRule(newEntity, 1001L, "Admin", "Factor adjustment");

      // Assert
      verify(statusFactorRuleDao, times(1)).updateById(any(TurnoverStatusFactorRuleEntity.class));

      ArgumentCaptor<TurnoverRuleChangeLogEntity> captor =
          ArgumentCaptor.forClass(TurnoverRuleChangeLogEntity.class);
      verify(changeLogDao, times(1)).insert(captor.capture());

      TurnoverRuleChangeLogEntity changeLog = captor.getValue();
      assertThat(changeLog.getOperationType()).isEqualTo(2); // UPDATE
      assertThat(changeLog.getOldValue()).isEqualTo("{\"factor\":100}");
      assertThat(changeLog.getNewValue()).isEqualTo("{\"factor\":80}");
    }

    @Test
    @DisplayName("當規則不存在時應該拋出異常")
    void whenRuleNotFound_ShouldThrowException() {
      // Arrange
      TurnoverStatusFactorRuleEntity entity =
          TurnoverTestFixture.createStatusFactorRule(1, new BigDecimal("100.00"));
      entity.setRuleId(999L);
      when(statusFactorRuleDao.selectById(999L)).thenReturn(null);

      // Act & Assert
      assertThatThrownBy(() -> manager.updateRule(entity, 1001L, "Admin", "Update"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Rule not found");

      verify(statusFactorRuleDao, never()).updateById(any(TurnoverStatusFactorRuleEntity.class));
    }

    @Test
    @DisplayName("當版本衝突時應該拋出樂觀鎖異常")
    void whenVersionConflict_ShouldThrowOptimisticLockException() {
      // Arrange
      TurnoverStatusFactorRuleEntity oldEntity =
          TurnoverTestFixture.createStatusFactorRule(1, new BigDecimal("100.00"));
      oldEntity.setRuleId(1L);
      oldEntity.setDeleted(false);

      TurnoverStatusFactorRuleEntity newEntity =
          TurnoverTestFixture.createStatusFactorRule(1, new BigDecimal("80.00"));
      newEntity.setRuleId(1L);

      when(statusFactorRuleDao.selectById(1L)).thenReturn(oldEntity);
      when(statusFactorRuleDao.updateById(any(TurnoverStatusFactorRuleEntity.class))).thenReturn(0);

      // Act & Assert
      assertThatThrownBy(() -> manager.updateRule(newEntity, 1001L, "Admin", "Update"))
          .isInstanceOf(OptimisticLockingFailureException.class)
          .hasMessageContaining("Version conflict");
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
      TurnoverStatusFactorRuleEntity entity =
          TurnoverTestFixture.createStatusFactorRule(1, new BigDecimal("100.00"));
      entity.setRuleId(1L);
      entity.setDeleted(false);

      when(statusFactorRuleDao.selectById(1L)).thenReturn(entity);
      when(statusFactorRuleDao.updateById(any(TurnoverStatusFactorRuleEntity.class))).thenReturn(1);
      when(objectMapper.writeValueAsString(any())).thenReturn("{\"ruleId\":1}");

      // Act
      manager.deleteRule(1L, 1001L, "Admin", "No longer needed");

      // Assert
      verify(statusFactorRuleDao, times(1)).updateById(any(TurnoverStatusFactorRuleEntity.class));
      assertThat(entity.getDeleted()).isTrue();

      ArgumentCaptor<TurnoverRuleChangeLogEntity> captor =
          ArgumentCaptor.forClass(TurnoverRuleChangeLogEntity.class);
      verify(changeLogDao, times(1)).insert(captor.capture());

      TurnoverRuleChangeLogEntity changeLog = captor.getValue();
      assertThat(changeLog.getOperationType()).isEqualTo(3); // DELETE
    }

    @Test
    @DisplayName("當規則已刪除時應該拋出異常")
    void whenRuleAlreadyDeleted_ShouldThrowException() {
      // Arrange
      TurnoverStatusFactorRuleEntity entity =
          TurnoverTestFixture.createStatusFactorRule(1, new BigDecimal("100.00"));
      entity.setRuleId(1L);
      entity.setDeleted(true);

      when(statusFactorRuleDao.selectById(1L)).thenReturn(entity);

      // Act & Assert
      assertThatThrownBy(() -> manager.deleteRule(1L, 1001L, "Admin", "Delete"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("already deleted");
    }
  }

  // ===== evictCache() 測試 =====

  @Test
  @DisplayName("evictCache 應該執行無異常")
  void evictCacheShouldExecuteWithoutException() {
    // Act & Assert
    manager.evictCache(1L);
  }
}
