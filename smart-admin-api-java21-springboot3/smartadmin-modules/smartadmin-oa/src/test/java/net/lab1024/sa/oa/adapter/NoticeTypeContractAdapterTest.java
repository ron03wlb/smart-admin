package net.lab1024.sa.oa.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.vavr.control.Option;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.lab1024.sa.api.oa.dto.NoticeTypeDTO;
import net.lab1024.sa.oa.notice.domain.vo.NoticeTypeVO;
import net.lab1024.sa.oa.notice.service.NoticeTypeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * NoticeTypeContractAdapter 單元測試
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
class NoticeTypeContractAdapterTest {

  @Mock private NoticeTypeService noticeTypeService;

  @InjectMocks private NoticeTypeContractAdapter adapter;

  // ==================== getAll 測試 ====================

  @Test
  void testGetAll_Success() {
    // Given
    NoticeTypeVO vo1 = new NoticeTypeVO();
    vo1.setNoticeTypeId(1L);
    vo1.setNoticeTypeName("系統通知");

    NoticeTypeVO vo2 = new NoticeTypeVO();
    vo2.setNoticeTypeId(2L);
    vo2.setNoticeTypeName("活動通知");

    NoticeTypeVO vo3 = new NoticeTypeVO();
    vo3.setNoticeTypeId(3L);
    vo3.setNoticeTypeName("維護通知");

    List<NoticeTypeVO> voList = Arrays.asList(vo1, vo2, vo3);
    when(noticeTypeService.getAll()).thenReturn(voList);

    // When
    List<NoticeTypeDTO> result = adapter.getAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).hasSize(3);
    assertThat(result.get(0).getNoticeTypeName()).isEqualTo("系統通知");
    assertThat(result.get(1).getNoticeTypeName()).isEqualTo("活動通知");
    assertThat(result.get(2).getNoticeTypeName()).isEqualTo("維護通知");
    verify(noticeTypeService).getAll();
  }

  @Test
  void testGetAll_EmptyResult() {
    // Given
    when(noticeTypeService.getAll()).thenReturn(Collections.emptyList());

    // When
    List<NoticeTypeDTO> result = adapter.getAll();

    // Then
    assertThat(result).isNotNull();
    assertThat(result).isEmpty();
    verify(noticeTypeService).getAll();
  }

  // ==================== getById 測試 ====================

  @Test
  void testGetById_Found() {
    // Given
    Long noticeTypeId = 1L;
    NoticeTypeVO vo = new NoticeTypeVO();
    vo.setNoticeTypeId(noticeTypeId);
    vo.setNoticeTypeName("系統通知");

    when(noticeTypeService.getByNoticeTypeId(noticeTypeId)).thenReturn(vo);

    // When
    Option<NoticeTypeDTO> result = adapter.getById(noticeTypeId);

    // Then
    assertThat(result.isDefined()).isTrue();
    assertThat(result.get().getNoticeTypeId()).isEqualTo(noticeTypeId);
    assertThat(result.get().getNoticeTypeName()).isEqualTo("系統通知");
    verify(noticeTypeService).getByNoticeTypeId(noticeTypeId);
  }

  @Test
  void testGetById_NotFound() {
    // Given
    Long noticeTypeId = 999L;
    when(noticeTypeService.getByNoticeTypeId(noticeTypeId)).thenReturn(null);

    // When
    Option<NoticeTypeDTO> result = adapter.getById(noticeTypeId);

    // Then
    assertThat(result.isEmpty()).isTrue();
    verify(noticeTypeService).getByNoticeTypeId(noticeTypeId);
  }

  @Test
  void testGetById_NullParameter() {
    // When & Then
    assertThatThrownBy(() -> adapter.getById(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("noticeTypeId cannot be null");
  }
}
