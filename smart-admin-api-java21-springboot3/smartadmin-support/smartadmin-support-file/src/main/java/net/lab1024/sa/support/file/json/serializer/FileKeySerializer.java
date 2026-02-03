package net.lab1024.sa.support.file.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.file.service.FileService;
import org.apache.commons.lang3.StringUtils;

/**
 * 文件key进行序列化对象
 *
 * <p>注意：此序列化器由 Jackson 实例化，需要无参构造函数。 FileService 依赖通过 Spring 的 HandlerInstantiator 注入。
 *
 * @author 1024创新实验室: 罗伊
 * @since 2020/8/15 22:06 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public class FileKeySerializer extends JsonSerializer<String> {

  private FileService fileService;

  /** 无参构造函数供 Jackson 使用 */
  public FileKeySerializer() {}

  /** 构造函数注入供 Spring HandlerInstantiator 使用 */
  public FileKeySerializer(FileService fileService) {
    this.fileService = fileService;
  }

  @Override
  public void serialize(
      String value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
      throws IOException {
    if (StringUtils.isEmpty(value)) {
      jsonGenerator.writeString(value);
      return;
    }
    if (fileService == null) {
      jsonGenerator.writeString(value);
      return;
    }
    ResponseDTO<String> responseDTO = fileService.getFileUrl(value);
    if (responseDTO.getOk()) {
      jsonGenerator.writeString(responseDTO.getData());
      return;
    }
    jsonGenerator.writeString(value);
  }
}
