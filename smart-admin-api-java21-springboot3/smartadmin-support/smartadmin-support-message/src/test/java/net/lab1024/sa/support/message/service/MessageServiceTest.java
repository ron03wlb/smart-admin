package net.lab1024.sa.support.message.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.time.LocalDateTime;
import java.util.List;
import net.lab1024.sa.common.core.domain.enumeration.UserTypeEnum;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.message.dao.MessageDao;
import net.lab1024.sa.support.message.domain.MessageEntity;
import net.lab1024.sa.support.message.domain.MessageQueryForm;
import net.lab1024.sa.support.message.domain.MessageSendForm;
import net.lab1024.sa.support.message.domain.MessageVO;
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
 * MessageService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>query - 分頁查詢消息
 *   <li>getUnreadCount - 獲取未讀消息數
 *   <li>updateReadFlag - 標記消息已讀
 *   <li>sendMessage - 發送消息
 *   <li>delete - 刪除消息
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessageService 單元測試")
class MessageServiceTest {

  @Mock private MessageDao messageDao;

  @Mock private MessageManager messageManager;

  @InjectMocks private MessageService messageService;

  @Captor private ArgumentCaptor<List<MessageEntity>> messageEntityListCaptor;

  // ==================== query 測試 ====================

  @Nested
  @DisplayName("query 分頁查詢測試")
  class QueryTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      MessageQueryForm form = new MessageQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      MessageVO vo = createTestMessageVO();
      when(messageDao.query(any(Page.class), any(MessageQueryForm.class))).thenReturn(List.of(vo));

      // When
      PageResult<MessageVO> result = messageService.query(form);

      // Then
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("空結果：應該返回空分頁")
    void shouldReturnEmptyPageResult() {
      // Given
      MessageQueryForm form = new MessageQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      when(messageDao.query(any(Page.class), any(MessageQueryForm.class))).thenReturn(List.of());

      // When
      PageResult<MessageVO> result = messageService.query(form);

      // Then
      assertThat(result).isNotNull();
    }
  }

  // ==================== getUnreadCount 測試 ====================

  @Nested
  @DisplayName("getUnreadCount 獲取未讀數測試")
  class GetUnreadCountTest {

    @Test
    @DisplayName("正常情況：應該返回未讀消息數")
    void shouldReturnUnreadCount() {
      // Given
      UserTypeEnum userType = UserTypeEnum.ADMIN_EMPLOYEE;
      Long userId = 1L;
      when(messageDao.getUnreadCount(userType.getValue(), userId)).thenReturn(5L);

      // When
      Long count = messageService.getUnreadCount(userType, userId);

      // Then
      assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("無未讀：應該返回 0")
    void shouldReturnZeroWhenNoUnread() {
      // Given
      UserTypeEnum userType = UserTypeEnum.ADMIN_EMPLOYEE;
      Long userId = 1L;
      when(messageDao.getUnreadCount(userType.getValue(), userId)).thenReturn(0L);

      // When
      Long count = messageService.getUnreadCount(userType, userId);

      // Then
      assertThat(count).isEqualTo(0L);
    }
  }

  // ==================== updateReadFlag 測試 ====================

  @Nested
  @DisplayName("updateReadFlag 標記已讀測試")
  class UpdateReadFlagTest {

    @Test
    @DisplayName("正常情況：應該成功標記已讀")
    void shouldUpdateReadFlagSuccessfully() {
      // Given
      Long messageId = 1L;
      UserTypeEnum userType = UserTypeEnum.ADMIN_EMPLOYEE;
      Long receiverUserId = 1L;

      when(messageDao.updateReadFlag(messageId, userType.getValue(), receiverUserId, true))
          .thenReturn(1);

      // When
      messageService.updateReadFlag(messageId, userType, receiverUserId);

      // Then
      verify(messageDao).updateReadFlag(messageId, userType.getValue(), receiverUserId, true);
    }
  }

  // ==================== sendMessage 測試 ====================

  @Nested
  @DisplayName("sendMessage 發送消息測試")
  class SendMessageTest {

    @Test
    @DisplayName("正常情況：應該成功發送消息")
    void shouldSendMessageSuccessfully() {
      // Given
      MessageSendForm form = createTestMessageSendForm();
      when(messageManager.saveBatch(anyList())).thenReturn(true);

      // When
      messageService.sendMessage(form);

      // Then
      verify(messageManager).saveBatch(messageEntityListCaptor.capture());
      List<MessageEntity> captured = messageEntityListCaptor.getValue();
      assertThat(captured).hasSize(1);
      assertThat(captured.get(0).getTitle()).isEqualTo("測試消息");
    }

    @Test
    @DisplayName("批量發送：應該成功發送多條消息")
    void shouldSendMultipleMessagesSuccessfully() {
      // Given
      MessageSendForm form1 = createTestMessageSendForm();
      MessageSendForm form2 = createTestMessageSendForm();
      form2.setTitle("測試消息2");
      when(messageManager.saveBatch(anyList())).thenReturn(true);

      // When
      messageService.sendMessage(List.of(form1, form2));

      // Then
      verify(messageManager).saveBatch(messageEntityListCaptor.capture());
      List<MessageEntity> captured = messageEntityListCaptor.getValue();
      assertThat(captured).hasSize(2);
    }

    @Test
    @DisplayName("驗證失敗：應該拋出異常")
    void shouldThrowExceptionWhenValidationFails() {
      // Given
      MessageSendForm form = new MessageSendForm();
      // 缺少必填欄位

      // When & Then
      assertThatThrownBy(() -> messageService.sendMessage(form))
          .isInstanceOf(RuntimeException.class)
          .hasMessageContaining("send msg error");
    }
  }

  // ==================== delete 測試 ====================

  @Nested
  @DisplayName("delete 刪除消息測試")
  class DeleteTest {

    @Test
    @DisplayName("正常情況：應該成功刪除消息")
    void shouldDeleteMessageSuccessfully() {
      // Given
      Long messageId = 1L;
      when(messageDao.deleteById(messageId)).thenReturn(1);

      // When
      ResponseDTO<String> result = messageService.delete(messageId);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(messageDao).deleteById(messageId);
    }

    @Test
    @DisplayName("messageId 為空：應該返回錯誤")
    void shouldReturnErrorWhenMessageIdIsNull() {
      // When
      ResponseDTO<String> result = messageService.delete(null);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(messageDao, never()).deleteById(any(Long.class));
    }
  }

  // ==================== Helper Methods ====================

  private MessageVO createTestMessageVO() {
    MessageVO vo = new MessageVO();
    vo.setMessageId(1L);
    vo.setMessageType(1);
    vo.setReceiverUserType(UserTypeEnum.ADMIN_EMPLOYEE.getValue());
    vo.setReceiverUserId(1L);
    vo.setTitle("測試消息");
    vo.setContent("這是測試消息內容");
    vo.setReadFlag(false);
    vo.setCreateTime(LocalDateTime.now());
    return vo;
  }

  private MessageSendForm createTestMessageSendForm() {
    MessageSendForm form = new MessageSendForm();
    form.setMessageType(1);
    form.setReceiverUserType(UserTypeEnum.ADMIN_EMPLOYEE.getValue());
    form.setReceiverUserId(1L);
    form.setTitle("測試消息");
    form.setContent("這是測試消息內容");
    return form;
  }
}
