package net.lab1024.sa.admin.module.system.message;

import cn.dev33.satoken.annotation.SaCheckPermission;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.lab1024.sa.admin.constant.AdminSwaggerTagConst;
import net.lab1024.sa.base.module.support.message.domain.MessageQueryForm;
import net.lab1024.sa.base.module.support.message.domain.MessageSendForm;
import net.lab1024.sa.base.module.support.message.domain.MessageVO;
import net.lab1024.sa.base.module.support.message.service.MessageService;
import net.lab1024.sa.common.core.domain.PageResult;
import net.lab1024.sa.common.core.domain.ResponseDTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后管 消息路由
 *
 * @author: 卓大
 * @since: 2025/04/09 20:55
 */
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
@Tag(name = AdminSwaggerTagConst.System.SYSTEM_MESSAGE)
@RequiredArgsConstructor
@RestController
public class AdminMessageController {

  private final MessageService messageService;

  @Operation(summary = "通知消息-新建  @author 卓大")
  @PostMapping("/message/sendMessages")
  @SaCheckPermission("system:message:send")
  public ResponseDTO<String> sendMessages(@RequestBody @Valid List<MessageSendForm> messageList) {
    messageService.sendMessage(messageList);
    return ResponseDTO.ok();
  }

  @Operation(summary = "通知消息-分页查询   @author 卓大")
  @PostMapping("/message/query")
  @SaCheckPermission("system:message:query")
  public ResponseDTO<PageResult<MessageVO>> query(@RequestBody @Valid MessageQueryForm queryForm) {
    return ResponseDTO.ok(messageService.query(queryForm));
  }

  @Operation(summary = "通知消息-删除   @author 卓大")
  @GetMapping("/message/delete/{messageId}")
  @SaCheckPermission("system:message:delete")
  public ResponseDTO<String> delete(@PathVariable Long messageId) {
    return messageService.delete(messageId);
  }
}
