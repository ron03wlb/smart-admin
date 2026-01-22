package net.lab1024.sa.foundation.json.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * 字典反序列化
 *
 * @author 1024创新实验室: 罗伊
 * @since 2022-08-12 22:17:53 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
public class DictDataDeserializer extends JsonDeserializer<String> {

  @Override
  public String deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
      throws IOException {
    List<String> list = new ArrayList<>();
    ObjectCodec objectCodec = jsonParser.getCodec();
    JsonNode listOrObjectNode = objectCodec.readTree(jsonParser);
    String deserialize;
    try {
      if (listOrObjectNode.isArray()) {
        for (JsonNode node : listOrObjectNode) {
          list.add(node.asText());
        }
      } else {
        list.add(listOrObjectNode.asText());
      }
      deserialize = String.join(",", list);
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error(e.getMessage(), e);
      }
      deserialize = listOrObjectNode.asText();
    }
    return deserialize;
  }
}
