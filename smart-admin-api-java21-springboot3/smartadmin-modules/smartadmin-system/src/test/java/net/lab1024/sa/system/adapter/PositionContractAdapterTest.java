package net.lab1024.sa.system.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vavr.control.Option;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.api.system.dto.PositionDTO;
import net.lab1024.sa.system.position.dao.PositionDao;
import net.lab1024.sa.system.position.domain.entity.PositionEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PositionContractAdapter 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>所有契約方法的正常流程
 *   <li>異常參數處理（null 參數）
 *   <li>邊界情況（空結果、未找到）
 *   <li>Vavr Option 正確使用
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
class PositionContractAdapterTest {

  @Mock private PositionDao positionDao;

  @InjectMocks private PositionContractAdapter adapter;

  // ==================== getById 測試 ====================

  @Test
  void testGetById_Found() {
    // Given
    Long positionId = 1L;
    PositionEntity entity = new PositionEntity();
    entity.setPositionId(positionId);
    entity.setPositionName("軟體工程師");
    entity.setPositionLevel(2);
    entity.setRemark("負責系統開發");

    when(positionDao.selectById(positionId)).thenReturn(entity);

    // When
    Option<PositionDTO> result = adapter.getById(positionId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getPositionId()).isEqualTo(positionId);
    assertThat(result.get().getPositionName()).isEqualTo("軟體工程師");
    assertThat(result.get().getPositionLevel()).isEqualTo(2);
    assertThat(result.get().getRemark()).isEqualTo("負責系統開發");
    verify(positionDao).selectById(positionId);
  }

  @Test
  void testGetById_NotFound() {
    // Given
    Long positionId = 999L;
    when(positionDao.selectById(positionId)).thenReturn(null);

    // When
    Option<PositionDTO> result = adapter.getById(positionId);

    // Then
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void testGetById_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("positionId cannot be null");
  }

  // ==================== listAll 測試 ====================

  @Test
  void testListAll_Success() {
    // Given
    PositionEntity entity1 = new PositionEntity();
    entity1.setPositionId(1L);
    entity1.setPositionName("軟體工程師");
    entity1.setPositionLevel(2);

    PositionEntity entity2 = new PositionEntity();
    entity2.setPositionId(2L);
    entity2.setPositionName("高級工程師");
    entity2.setPositionLevel(3);

    PositionEntity entity3 = new PositionEntity();
    entity3.setPositionId(3L);
    entity3.setPositionName("架構師");
    entity3.setPositionLevel(4);

    List<PositionEntity> entities = Arrays.asList(entity1, entity2, entity3);
    when(positionDao.selectList(null)).thenReturn(entities);

    // When
    List<PositionDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getPositionId()).isEqualTo(1L);
    assertThat(result.get(0).getPositionName()).isEqualTo("軟體工程師");
    assertThat(result.get(0).getPositionLevel()).isEqualTo(2);
    assertThat(result.get(1).getPositionId()).isEqualTo(2L);
    assertThat(result.get(2).getPositionId()).isEqualTo(3L);
    verify(positionDao).selectList(null);
  }

  @Test
  void testListAll_EmptyResult() {
    // Given
    when(positionDao.selectList(null)).thenReturn(Collections.emptyList());

    // When
    List<PositionDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
  }

  @Test
  void testListAll_SinglePosition() {
    // Given - 只有一個職位
    PositionEntity entity = new PositionEntity();
    entity.setPositionId(1L);
    entity.setPositionName("實習生");
    entity.setPositionLevel(1);

    List<PositionEntity> entities = Collections.singletonList(entity);
    when(positionDao.selectList(null)).thenReturn(entities);

    // When
    List<PositionDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getPositionName()).isEqualTo("實習生");
  }
}
