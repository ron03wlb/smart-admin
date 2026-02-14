package net.lab1024.sa.support.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDateTime;
import java.util.List;
import net.lab1024.sa.common.core.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.common.core.domain.request.RequestUser;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.feedback.dao.FeedbackDao;
import net.lab1024.sa.support.feedback.domain.FeedbackAddForm;
import net.lab1024.sa.support.feedback.domain.FeedbackEntity;
import net.lab1024.sa.support.feedback.domain.FeedbackQueryForm;
import net.lab1024.sa.support.feedback.domain.FeedbackVO;
import net.lab1024.sa.support.feedback.service.FeedbackService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * FeedbackService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>query - 分頁查詢意見反饋
 *   <li>add - 新增反饋
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeedbackService 單元測試")
class FeedbackServiceTest {

  @Mock private FeedbackDao feedbackDao;

  @InjectMocks private FeedbackService feedbackService;

  @Captor private ArgumentCaptor<FeedbackEntity> entityCaptor;

  // ==================== query 測試 ====================

  @Nested
  @DisplayName("query 分頁查詢測試")
  class QueryTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      FeedbackQueryForm form = new FeedbackQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      FeedbackVO vo = createTestFeedbackVO();
      when(feedbackDao.queryPage(any(Page.class), any(FeedbackQueryForm.class)))
          .thenReturn(List.of(vo));

      // When
      ResponseDTO<PageResult<FeedbackVO>> result = feedbackService.query(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
    }

    @Test
    @DisplayName("空結果：應該返回空分頁")
    void shouldReturnEmptyPageResult() {
      // Given
      FeedbackQueryForm form = new FeedbackQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      when(feedbackDao.queryPage(any(Page.class), any(FeedbackQueryForm.class)))
          .thenReturn(List.of());

      // When
      ResponseDTO<PageResult<FeedbackVO>> result = feedbackService.query(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData().getEmptyFlag()).isTrue();
    }
  }

  // ==================== add 測試 ====================

  @Nested
  @DisplayName("add 新增反饋測試")
  class AddTest {

    @Test
    @DisplayName("正常情況：應該成功新增反饋")
    void shouldAddFeedbackSuccessfully() {
      // Given
      FeedbackAddForm form = createTestFeedbackAddForm();
      RequestUser requestUser = createTestRequestUser();

      // When
      ResponseDTO<String> result = feedbackService.add(form, requestUser);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(feedbackDao).insert(entityCaptor.capture());

      FeedbackEntity captured = entityCaptor.getValue();
      assertThat(captured.getFeedbackContent()).isEqualTo("測試反饋內容");
      assertThat(captured.getUserType()).isEqualTo(UserTypeEnum.ADMIN_EMPLOYEE.getValue());
      assertThat(captured.getUserId()).isEqualTo(1L);
      assertThat(captured.getUserName()).isEqualTo("admin");
    }
  }

  // ==================== Helper Methods ====================

  private FeedbackVO createTestFeedbackVO() {
    FeedbackVO vo = new FeedbackVO();
    vo.setFeedbackId(1L);
    vo.setFeedbackContent("測試反饋內容");
    vo.setUserType(UserTypeEnum.ADMIN_EMPLOYEE.getValue());
    vo.setUserId(1L);
    vo.setUserName("admin");
    vo.setCreateTime(LocalDateTime.now());
    return vo;
  }

  private FeedbackAddForm createTestFeedbackAddForm() {
    FeedbackAddForm form = new FeedbackAddForm();
    form.setFeedbackContent("測試反饋內容");
    form.setFeedbackAttachment("attachment.jpg");
    return form;
  }

  private RequestUser createTestRequestUser() {
    RequestUser user = mock(RequestUser.class);
    when(user.getUserType()).thenReturn(UserTypeEnum.ADMIN_EMPLOYEE);
    when(user.getUserId()).thenReturn(1L);
    when(user.getUserName()).thenReturn("admin");
    return user;
  }
}
