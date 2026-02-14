package net.lab1024.sa.business.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.vavr.control.Option;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.api.business.dto.BrandDTO;
import net.lab1024.sa.business.brand.dao.BrandDao;
import net.lab1024.sa.business.brand.domain.entity.BrandEntity;
import net.lab1024.sa.business.brand.domain.vo.BrandVO;
import net.lab1024.sa.business.brand.service.BrandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * BrandContractAdapter 單元測試
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
class BrandContractAdapterTest {

  @Mock private BrandService brandService;

  @Mock private BrandDao brandDao;

  @InjectMocks private BrandContractAdapter adapter;

  // ==================== listAll 測試 ====================

  @Test
  void testListAll_Success() {
    // Given
    BrandEntity entity1 = new BrandEntity();
    entity1.setBrandId(1L);
    entity1.setBrandName("Apple");
    entity1.setDeletedFlag(Boolean.FALSE);

    BrandEntity entity2 = new BrandEntity();
    entity2.setBrandId(2L);
    entity2.setBrandName("Samsung");
    entity2.setDeletedFlag(Boolean.FALSE);

    List<BrandEntity> entityList = Arrays.asList(entity1, entity2);
    when(brandDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(entityList);

    // When
    List<BrandDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getBrandName()).isEqualTo("Apple");
    assertThat(result.get(1).getBrandName()).isEqualTo("Samsung");
    verify(brandDao).selectList(any(LambdaQueryWrapper.class));
  }

  @Test
  void testListAll_EmptyResult() {
    // Given
    when(brandDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

    // When
    List<BrandDTO> result = adapter.listAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(brandDao).selectList(any(LambdaQueryWrapper.class));
  }

  // ==================== getById 測試 ====================

  @Test
  void testGetById_Found() {
    // Given
    Long brandId = 1L;
    BrandVO vo = new BrandVO();
    vo.setBrandId(brandId);
    vo.setBrandName("Apple");

    when(brandService.getById(brandId)).thenReturn(Option.of(vo));

    // When
    Option<BrandDTO> result = adapter.getById(brandId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getBrandId()).isEqualTo(brandId);
    assertThat(result.get().getBrandName()).isEqualTo("Apple");
    verify(brandService).getById(brandId);
  }

  @Test
  void testGetById_NotFound() {
    // Given
    Long brandId = 999L;
    when(brandService.getById(brandId)).thenReturn(Option.none());

    // When
    Option<BrandDTO> result = adapter.getById(brandId);

    // Then
    assertThat(result.isEmpty()).isTrue();
    verify(brandService).getById(brandId);
  }

  @Test
  void testGetById_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("brandId cannot be null");
  }

  // ==================== queryByNameKeyword 測試 ====================

  @Test
  void testQueryByNameKeyword_Success() {
    // Given
    String keyword = "App";
    BrandEntity entity = new BrandEntity();
    entity.setBrandId(1L);
    entity.setBrandName("Apple");
    entity.setDeletedFlag(Boolean.FALSE);

    when(brandDao.selectList(any(LambdaQueryWrapper.class)))
        .thenReturn(Collections.singletonList(entity));

    // When
    List<BrandDTO> result = adapter.queryByNameKeyword(keyword);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getBrandName()).isEqualTo("Apple");
    verify(brandDao).selectList(any(LambdaQueryWrapper.class));
  }

  @Test
  void testQueryByNameKeyword_EmptyResult() {
    // Given
    String keyword = "NonExistent";
    when(brandDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

    // When
    List<BrandDTO> result = adapter.queryByNameKeyword(keyword);

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(brandDao).selectList(any(LambdaQueryWrapper.class));
  }

  @Test
  void testQueryByNameKeyword_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.queryByNameKeyword(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("nameKeyword cannot be null or blank");
  }

  @Test
  void testQueryByNameKeyword_BlankParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.queryByNameKeyword("  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("nameKeyword cannot be null or blank");
  }
}
