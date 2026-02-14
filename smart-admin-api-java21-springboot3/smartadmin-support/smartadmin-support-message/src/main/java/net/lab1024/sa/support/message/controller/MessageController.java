package net.lab1024.sa.support.message.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.common.core.domain.request.RequestUser;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.core.util.SmartRequestUtil;
import net.lab1024.sa.common.swagger.constant.SwaggerTagConst;
import net.lab1024.sa.common.web.web.base.SupportBaseController;
import net.lab1024.sa.support.message.domain.MessageQueryForm;
import net.lab1024.sa.support.message.domain.MessageVO;
import net.lab1024.sa.support.message.service.MessageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息
 *
 * @author luoyi
 * @since 2024/06/22 20:20
 */
@RestController
@RequiredArgsConstructor
@Tag(name = SwaggerTagConst.Support.MESSAGE)
public class MessageController extends SupportBaseController {

  private static final String USER_NOT_LOGIN = "用户未登录";

  private final MessageService messageService;

  @Operation(summary = "分页查询我的消息 @luoyi")
  @PostMapping("/message/queryMyMessage")
  public ResponseDTO<PageResult<MessageVO>> query(
      @RequestBody @Valid final MessageQueryForm queryForm) {
    final RequestUser user = SmartRequestUtil.getRequestUser();
    if (user == null) {
      return ResponseDTO.userErrorParam(USER_NOT_LOGIN);
    }

    queryForm.setSearchCount(false);
    queryForm.setReceiverUserId(user.getUserId());
    queryForm.setReceiverUserType(user.getUserType().getValue());
    return ResponseDTO.ok(messageService.query(queryForm));
  }

  @Operation(summary = "查询未读消息数量 @luoyi")
  @GetMapping("/message/getUnreadCount")
  public ResponseDTO<Long> getUnreadCount() {
    final RequestUser user = SmartRequestUtil.getRequestUser();
    if (user == null) {
      return ResponseDTO.userErrorParam(USER_NOT_LOGIN);
    }
    return ResponseDTO.ok(messageService.getUnreadCount(user.getUserType(), user.getUserId()));
  }

  @Operation(summary = "更新已读 @luoyi")
  @GetMapping("/message/read/{messageId}")
  public ResponseDTO<String> updateReadFlag(@PathVariable final Long messageId) {
    final RequestUser user = SmartRequestUtil.getRequestUser();
    if (user == null) {
      return ResponseDTO.userErrorParam(USER_NOT_LOGIN);
    }

    messageService.updateReadFlag(messageId, user.getUserType(), user.getUserId());
    return ResponseDTO.ok();
  }
}
