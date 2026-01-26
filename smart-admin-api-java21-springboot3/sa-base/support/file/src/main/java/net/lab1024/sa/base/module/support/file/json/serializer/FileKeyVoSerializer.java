package net.lab1024.sa.base.module.support.file.json.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import net.lab1024.sa.base.module.support.file.domain.vo.FileVO;
import net.lab1024.sa.base.module.support.file.service.FileService;
import org.apache.commons.lang3.StringUtils;

/**
 * 文件key进行序列化对象
 *
 * <p>注意：此序列化器由 Jackson 实例化，需要无参构造函数。 FileService 依赖通过 Spring 的 HandlerInstantiator 注入。
 *
 * @author 1024创新实验室: 罗伊
 * @since 2020/8/15 22:06 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public class FileKeyVoSerializer extends JsonSerializer<String> {

  private FileService fileService;

  /** 无参构造函数供 Jackson 使用 */
  public FileKeyVoSerializer() {}

  /** 构造函数注入供 Spring HandlerInstantiator 使用 */
  public FileKeyVoSerializer(FileService fileService) {
    this.fileService = fileService;
  }

  @Override
  public void serialize(
      String value, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
      throws IOException {
    if (StringUtils.isEmpty(value)) {
      jsonGenerator.writeObject(Lists.newArrayList());
      return;
    }
    if (fileService == null) {
      jsonGenerator.writeString(value);
      return;
    }
    String[] fileKeyArray = value.split(",");
    List<String> fileKeyList = Arrays.asList(fileKeyArray);
    List<FileVO> fileKeyVOList = fileService.getFileList(fileKeyList);
    jsonGenerator.writeObject(fileKeyVOList);
  }
}
