package net.lab1024.sa.base.module.support.message.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;
import net.lab1024.sa.base.core.util.SmartPageUtil;
import net.lab1024.sa.base.module.support.message.constant.MessageTemplateEnum;
import net.lab1024.sa.base.module.support.message.dao.MessageDao;
import net.lab1024.sa.base.module.support.message.domain.MessageEntity;
import net.lab1024.sa.base.module.support.message.domain.MessageQueryForm;
import net.lab1024.sa.base.module.support.message.domain.MessageSendForm;
import net.lab1024.sa.base.module.support.message.domain.MessageTemplateSendForm;
import net.lab1024.sa.base.module.support.message.domain.MessageVO;
import net.lab1024.sa.common.core.domain.PageResult;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import net.lab1024.sa.common.core.enumeration.UserTypeEnum;
import net.lab1024.sa.common.core.util.SmartBeanUtil;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.stereotype.Service;

/**
 * @author luoyi
 * @since 2024/6/27 12:14 上午
 */
@Service
@SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.LongVariable"})
public class MessageService {

  @Resource private MessageDao messageDao;

  @Resource private MessageManager messageManager;

  /** 分页查询 消息 */
  public PageResult<MessageVO> query(final MessageQueryForm queryForm) {
    final Page page = SmartPageUtil.convert2PageQuery(queryForm);
    final List<MessageVO> messageVOList = messageDao.query(page, queryForm);
    return SmartPageUtil.convert2PageResult(page, messageVOList);
  }

  /** 查询未读消息数量 */
  public Long getUnreadCount(final UserTypeEnum userType, final Long userId) {
    return messageDao.getUnreadCount(userType.getValue(), userId);
  }

  /** 更新已读状态 */
  public void updateReadFlag(
      final Long messageId, final UserTypeEnum userType, final Long receiverUserId) {
    messageDao.updateReadFlag(messageId, userType.getValue(), receiverUserId, true);
  }

  /** 发送【模板消息】 */
  public void sendTemplateMessage(final MessageTemplateSendForm... sendTemplateForms) {
    final List<MessageSendForm> sendFormList = Lists.newArrayList();
    for (final MessageTemplateSendForm sendTemplateForm : sendTemplateForms) {
      final MessageTemplateEnum msgTemplateEnum = sendTemplateForm.getMessageTemplateEnum();
      final StringSubstitutor stringSubstitutor =
          new StringSubstitutor(sendTemplateForm.getContentParam());
      final String content = stringSubstitutor.replace(msgTemplateEnum.getContent());

      final MessageSendForm messageSendForm = new MessageSendForm();
      messageSendForm.setMessageType(msgTemplateEnum.getMessageTypeEnum().getValue());
      messageSendForm.setReceiverUserType(sendTemplateForm.getReceiverUserType().getValue());
      messageSendForm.setReceiverUserId(sendTemplateForm.getReceiverUserId());
      messageSendForm.setTitle(msgTemplateEnum.getDesc());
      messageSendForm.setContent(content);
      messageSendForm.setDataId(sendTemplateForm.getDataId());
      sendFormList.add(messageSendForm);
    }
    this.sendMessage(sendFormList);
  }

  /** 发送消息 */
  public void sendMessage(final MessageSendForm... sendForms) {
    this.sendMessage(Lists.newArrayList(sendForms));
  }

  /** 批量发送通知消息 */
  public void sendMessage(final List<MessageSendForm> sendList) {
    for (final MessageSendForm sendDTO : sendList) {
      final String verify = SmartBeanUtil.verify(sendDTO);
      if (null != verify) {
        throw new RuntimeException("send msg error: " + verify);
      }
    }
    final List<MessageEntity> messageEntityList =
        sendList.stream()
            .map(
                e -> {
                  final MessageEntity messageEntity = new MessageEntity();
                  messageEntity.setMessageType(e.getMessageType());
                  messageEntity.setReceiverUserType(e.getReceiverUserType());
                  messageEntity.setReceiverUserId(e.getReceiverUserId());
                  messageEntity.setDataId(String.valueOf(e.getDataId()));
                  messageEntity.setTitle(e.getTitle());
                  messageEntity.setContent(e.getContent());
                  return messageEntity;
                })
            .collect(Collectors.toList());
    messageManager.saveBatch(messageEntityList);
  }

  // 删除消息
  public ResponseDTO<String> delete(final Long messageId) {
    if (messageId == null) {
      return ResponseDTO.userErrorParam();
    }
    messageDao.deleteById(messageId);
    return ResponseDTO.ok();
  }
}
