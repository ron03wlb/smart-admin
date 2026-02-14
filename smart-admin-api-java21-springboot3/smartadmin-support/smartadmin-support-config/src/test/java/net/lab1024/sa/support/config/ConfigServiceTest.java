package net.lab1024.sa.support.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.support.config.domain.ConfigAddForm;
import net.lab1024.sa.support.config.domain.ConfigEntity;
import net.lab1024.sa.support.config.domain.ConfigQueryForm;
import net.lab1024.sa.support.config.domain.ConfigUpdateForm;
import net.lab1024.sa.support.config.domain.ConfigVO;
import org.junit.jupiter.api.BeforeEach;
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
 * ConfigService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>queryConfigPage - 分頁查詢配置
 *   <li>getConfig - 獲取配置（從快取）
 *   <li>add - 添加配置
 *   <li>updateConfig - 更新配置
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConfigService 單元測試")
class ConfigServiceTest {

  @Mock private ConfigDao configDao;

  @InjectMocks private ConfigService configService;

  @Captor private ArgumentCaptor<ConfigEntity> configEntityCaptor;

  @BeforeEach
  void setUp() {
    // 使用 lenient 模式，因為不是所有測試都會觸發 selectList
    lenient().when(configDao.selectList(null)).thenReturn(List.of());
  }

  // ==================== queryConfigPage 測試 ====================

  @Nested
  @DisplayName("queryConfigPage 分頁查詢測試")
  class QueryConfigPageTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      ConfigQueryForm form = new ConfigQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      ConfigEntity entity = createTestConfigEntity();
      when(configDao.queryByPage(any(Page.class), any(ConfigQueryForm.class)))
          .thenReturn(List.of(entity));

      // When
      ResponseDTO<PageResult<ConfigVO>> result = configService.queryConfigPage(form);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).isNotNull();
    }
  }

  // ==================== add 測試 ====================

  @Nested
  @DisplayName("add 添加配置測試")
  class AddTest {

    @Test
    @DisplayName("正常情況：應該成功添加")
    void shouldAddSuccess() {
      // Given
      ConfigAddForm form = createTestAddForm();
      when(configDao.selectByKey(form.getConfigKey())).thenReturn(null);

      // When
      ResponseDTO<String> result = configService.add(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(configDao).insert(configEntityCaptor.capture());
    }

    @Test
    @DisplayName("Key 已存在時：應該返回錯誤")
    void shouldReturnErrorWhenKeyExists() {
      // Given
      ConfigAddForm form = createTestAddForm();
      when(configDao.selectByKey(form.getConfigKey())).thenReturn(createTestConfigEntity());

      // When
      ResponseDTO<String> result = configService.add(form);

      // Then
      assertThat(result.getOk()).isFalse();
      verify(configDao, never()).insert(configEntityCaptor.capture());
    }
  }

  // ==================== updateConfig 測試 ====================

  @Nested
  @DisplayName("updateConfig 更新配置測試")
  class UpdateConfigTest {

    @Test
    @DisplayName("正常情況：應該成功更新")
    void shouldUpdateSuccess() {
      // Given
      ConfigUpdateForm form = createTestUpdateForm();
      ConfigEntity existingEntity = createTestConfigEntity();
      when(configDao.selectById(form.getConfigId())).thenReturn(existingEntity);
      when(configDao.selectByKey(form.getConfigKey())).thenReturn(null);

      // When
      ResponseDTO<String> result = configService.updateConfig(form);

      // Then
      assertThat(result.getOk()).isTrue();
      verify(configDao).updateById(configEntityCaptor.capture());
    }

    @Test
    @DisplayName("配置不存在時：應該返回錯誤")
    void shouldReturnErrorWhenNotExists() {
      // Given
      ConfigUpdateForm form = createTestUpdateForm();
      when(configDao.selectById(form.getConfigId())).thenReturn(null);

      // When
      ResponseDTO<String> result = configService.updateConfig(form);

      // Then
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("Key 被其他配置佔用時：應該返回錯誤")
    void shouldReturnErrorWhenKeyUsedByOther() {
      // Given
      ConfigUpdateForm form = createTestUpdateForm();
      ConfigEntity existingEntity = createTestConfigEntity();
      ConfigEntity otherEntity = createTestConfigEntity();
      otherEntity.setConfigId(999L); // 不同 ID

      when(configDao.selectById(form.getConfigId())).thenReturn(existingEntity);
      when(configDao.selectByKey(form.getConfigKey())).thenReturn(otherEntity);

      // When
      ResponseDTO<String> result = configService.updateConfig(form);

      // Then
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== getConfig 測試 ====================

  @Nested
  @DisplayName("getConfig 獲取配置測試")
  class GetConfigTest {

    @Test
    @DisplayName("Key 為空時：應該返回 null")
    void shouldReturnNullWhenKeyEmpty() {
      // When
      ConfigVO result = configService.getConfig("");

      // Then
      assertThat(result).isNull();
    }

    @Test
    @DisplayName("Key 為 null 時：應該返回 null")
    void shouldReturnNullWhenKeyNull() {
      // When
      ConfigVO result = configService.getConfig((String) null);

      // Then
      assertThat(result).isNull();
    }
  }

  // ==================== Helper Methods ====================

  private ConfigEntity createTestConfigEntity() {
    ConfigEntity entity = new ConfigEntity();
    entity.setConfigId(1L);
    entity.setConfigKey("test_key");
    entity.setConfigValue("test_value");
    entity.setConfigName("Test Config");
    return entity;
  }

  private ConfigAddForm createTestAddForm() {
    ConfigAddForm form = new ConfigAddForm();
    form.setConfigKey("new_key");
    form.setConfigValue("new_value");
    form.setConfigName("New Config");
    return form;
  }

  private ConfigUpdateForm createTestUpdateForm() {
    ConfigUpdateForm form = new ConfigUpdateForm();
    form.setConfigId(1L);
    form.setConfigKey("test_key");
    form.setConfigValue("updated_value");
    form.setConfigName("Updated Config");
    return form;
  }
}
