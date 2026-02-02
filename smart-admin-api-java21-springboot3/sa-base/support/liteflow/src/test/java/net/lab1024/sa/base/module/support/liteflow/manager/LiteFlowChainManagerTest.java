package net.lab1024.sa.base.module.support.liteflow.manager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import net.lab1024.sa.base.module.support.liteflow.core.executor.SmartFlowExecutor;
import net.lab1024.sa.base.module.support.liteflow.dao.LiteFlowChainDao;
import net.lab1024.sa.base.module.support.liteflow.domain.entity.LiteFlowChainEntity;
import net.lab1024.sa.base.module.support.liteflow.domain.form.LiteFlowChainAddForm;
import net.lab1024.sa.base.module.support.liteflow.domain.form.LiteFlowChainUpdateForm;
import net.lab1024.sa.foundation.domain.code.UserErrorCode;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * LiteFlowChainManager 單元測試
 *
 * @author SmartAdmin Team
 * @since 2026-02-02
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LiteFlowChainManager 單元測試")
class LiteFlowChainManagerTest {

  @Mock private LiteFlowChainDao chainDao;
  @Mock private LiteFlowCacheManager cacheManager;
  @Mock private SmartFlowExecutor flowExecutor;

  @InjectMocks private LiteFlowChainManager chainManager;

  private static final Long TEST_USER_ID = 1L;
  private static final String TEST_USER_NAME = "test-user";
  private static final String TEST_CHAIN_CODE = "test-chain";

  private LiteFlowChainAddForm createTestAddForm() {
    LiteFlowChainAddForm form = new LiteFlowChainAddForm();
    form.setChainName("測試流程");
    form.setChainCode(TEST_CHAIN_CODE);
    form.setChainType(1);
    form.setChainData("THEN(a, b, c)");
    form.setRemark("測試備註");
    return form;
  }

  private LiteFlowChainEntity createTestEntity() {
    LiteFlowChainEntity entity = new LiteFlowChainEntity();
    entity.setChainId(1L);
    entity.setChainName("測試流程");
    entity.setChainCode(TEST_CHAIN_CODE);
    entity.setChainType(1);
    entity.setChainData("THEN(a, b, c)");
    entity.setVersion(1);
    entity.setStatus(1);
    entity.setDeletedFlag(0);
    entity.setRemark("測試備註");
    entity.setCreateUserId(TEST_USER_ID);
    entity.setCreateUserName(TEST_USER_NAME);
    entity.setCreateTime(LocalDateTime.now());
    return entity;
  }

  @BeforeEach
  void setUp() {
    // Reset mocks before each test
    reset(chainDao, cacheManager, flowExecutor);
  }

  @Test
  @DisplayName("添加流程 - 成功場景")
  void testAdd_Success() {
    // Given
    LiteFlowChainAddForm form = createTestAddForm();

    when(chainDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    when(chainDao.insert(any(LiteFlowChainEntity.class))).thenReturn(1);
    doNothing().when(cacheManager).evictChain(anyString());
    doNothing().when(flowExecutor).reloadRule();

    // When
    ResponseDTO<String> response = chainManager.add(form, TEST_USER_ID, TEST_USER_NAME);

    // Then
    assertTrue(response.getOk(), "添加應成功");
    verify(chainDao, times(1)).selectCount(any(LambdaQueryWrapper.class));
    verify(chainDao, times(1)).insert(any(LiteFlowChainEntity.class));
    verify(cacheManager, times(1)).evictChain(TEST_CHAIN_CODE);
    verify(flowExecutor, times(1)).reloadRule();
  }

  @Test
  @DisplayName("添加流程 - chainCode 已存在")
  void testAdd_DuplicateChainCode() {
    // Given
    LiteFlowChainAddForm form = createTestAddForm();

    when(chainDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

    // When
    ResponseDTO<String> response = chainManager.add(form, TEST_USER_ID, TEST_USER_NAME);

    // Then
    assertFalse(response.getOk(), "添加應失敗");
    assertEquals("流程編碼已存在", response.getMsg());
    verify(chainDao, times(1)).selectCount(any(LambdaQueryWrapper.class));
    verify(chainDao, never()).insert(any(LiteFlowChainEntity.class));
    verify(cacheManager, never()).evictChain(anyString());
    verify(flowExecutor, never()).reloadRule();
  }

  @Test
  @DisplayName("更新流程 - 成功場景")
  void testUpdate_Success() {
    // Given
    LiteFlowChainUpdateForm form = new LiteFlowChainUpdateForm();
    form.setChainId(1L);
    form.setChainName("更新後流程");
    form.setChainData("THEN(d, e, f)");
    form.setRemark("更新備註");

    LiteFlowChainEntity existingEntity = createTestEntity();

    when(chainDao.selectById(1L)).thenReturn(existingEntity);
    when(chainDao.updateById(any(LiteFlowChainEntity.class))).thenReturn(1);
    doNothing().when(cacheManager).evictChain(anyString());
    doNothing().when(flowExecutor).reloadRule();

    // When
    ResponseDTO<String> response = chainManager.update(form, TEST_USER_ID, TEST_USER_NAME);

    // Then
    assertTrue(response.getOk(), "更新應成功");
    assertEquals(2, existingEntity.getVersion(), "版本號應遞增");
    verify(chainDao, times(1)).selectById(1L);
    verify(chainDao, times(1)).updateById(any(LiteFlowChainEntity.class));
    verify(cacheManager, times(1)).evictChain(TEST_CHAIN_CODE);
    verify(flowExecutor, times(1)).reloadRule();
  }

  @Test
  @DisplayName("更新流程 - 流程不存在")
  void testUpdate_ChainNotFound() {
    // Given
    LiteFlowChainUpdateForm form = new LiteFlowChainUpdateForm();
    form.setChainId(999L);
    form.setChainName("不存在的流程");
    form.setChainData("THEN(a)");

    when(chainDao.selectById(999L)).thenReturn(null);

    // When
    ResponseDTO<String> response = chainManager.update(form, TEST_USER_ID, TEST_USER_NAME);

    // Then
    assertFalse(response.getOk(), "更新應失敗");
    assertEquals(UserErrorCode.DATA_NOT_EXIST.getCode(), response.getCode());
    verify(chainDao, times(1)).selectById(999L);
    verify(chainDao, never()).updateById(any(LiteFlowChainEntity.class));
    verify(cacheManager, never()).evictChain(anyString());
    verify(flowExecutor, never()).reloadRule();
  }

  @Test
  @DisplayName("更新流程 - 流程已被刪除")
  void testUpdate_ChainDeleted() {
    // Given
    LiteFlowChainUpdateForm form = new LiteFlowChainUpdateForm();
    form.setChainId(1L);
    form.setChainName("已刪除的流程");
    form.setChainData("THEN(a)");

    LiteFlowChainEntity deletedEntity = createTestEntity();
    deletedEntity.setDeletedFlag(1);

    when(chainDao.selectById(1L)).thenReturn(deletedEntity);

    // When
    ResponseDTO<String> response = chainManager.update(form, TEST_USER_ID, TEST_USER_NAME);

    // Then
    assertFalse(response.getOk(), "更新應失敗");
    assertEquals(UserErrorCode.DATA_NOT_EXIST.getCode(), response.getCode());
    verify(chainDao, times(1)).selectById(1L);
    verify(chainDao, never()).updateById(any(LiteFlowChainEntity.class));
  }

  @Test
  @DisplayName("刪除流程 - 成功場景（軟刪除）")
  void testDelete_Success() {
    // Given
    Long chainId = 1L;
    LiteFlowChainEntity existingEntity = createTestEntity();

    when(chainDao.selectById(chainId)).thenReturn(existingEntity);
    when(chainDao.updateById(any(LiteFlowChainEntity.class))).thenReturn(1);
    doNothing().when(cacheManager).evictChain(anyString());
    doNothing().when(flowExecutor).reloadRule();

    // When
    ResponseDTO<String> response = chainManager.delete(chainId, TEST_USER_ID, TEST_USER_NAME);

    // Then
    assertTrue(response.getOk(), "刪除應成功");
    assertEquals(1, existingEntity.getDeletedFlag(), "刪除標記應為 1");
    verify(chainDao, times(1)).selectById(chainId);
    verify(chainDao, times(1)).updateById(any(LiteFlowChainEntity.class));
    verify(cacheManager, times(1)).evictChain(TEST_CHAIN_CODE);
    verify(flowExecutor, times(1)).reloadRule();
  }

  @Test
  @DisplayName("刪除流程 - 流程不存在")
  void testDelete_ChainNotFound() {
    // Given
    Long chainId = 999L;

    when(chainDao.selectById(chainId)).thenReturn(null);

    // When
    ResponseDTO<String> response = chainManager.delete(chainId, TEST_USER_ID, TEST_USER_NAME);

    // Then
    assertFalse(response.getOk(), "刪除應失敗");
    assertEquals(UserErrorCode.DATA_NOT_EXIST.getCode(), response.getCode());
    verify(chainDao, times(1)).selectById(chainId);
    verify(chainDao, never()).updateById(any(LiteFlowChainEntity.class));
    verify(cacheManager, never()).evictChain(anyString());
    verify(flowExecutor, never()).reloadRule();
  }

  @Test
  @DisplayName("重載所有流程 - 成功場景")
  void testReloadAll_Success() {
    // Given
    doNothing().when(cacheManager).evictAll();
    doNothing().when(flowExecutor).reloadRule();

    // When
    ResponseDTO<String> response = chainManager.reloadAll();

    // Then
    assertTrue(response.getOk(), "重載應成功");
    verify(cacheManager, times(1)).evictAll();
    verify(flowExecutor, times(1)).reloadRule();
  }

  @Test
  @DisplayName("SmartReload 集成 - liteflowReload 方法")
  void testLiteflowReload() {
    // Given
    String args = "test-args";
    doNothing().when(cacheManager).evictAll();
    doNothing().when(flowExecutor).reloadRule();

    // When
    chainManager.liteflowReload(args);

    // Then
    verify(cacheManager, times(1)).evictAll();
    verify(flowExecutor, times(1)).reloadRule();
  }

  @Test
  @DisplayName("添加流程 - 驗證實體字段設置")
  void testAdd_EntityFieldsValidation() {
    // Given
    LiteFlowChainAddForm form = createTestAddForm();

    when(chainDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
    when(chainDao.insert(any(LiteFlowChainEntity.class)))
        .thenAnswer(
            invocation -> {
              LiteFlowChainEntity entity = invocation.getArgument(0);

              // 驗證實體字段
              assertEquals(form.getChainName(), entity.getChainName());
              assertEquals(form.getChainCode(), entity.getChainCode());
              assertEquals(form.getChainType(), entity.getChainType());
              assertEquals(form.getChainData(), entity.getChainData());
              assertEquals(1, entity.getVersion(), "初始版本應為 1");
              assertEquals(1, entity.getStatus(), "初始狀態應為啟用");
              assertEquals(0, entity.getDeletedFlag(), "刪除標記應為 0");
              assertEquals(form.getRemark(), entity.getRemark());
              assertEquals(TEST_USER_ID, entity.getCreateUserId());
              assertEquals(TEST_USER_NAME, entity.getCreateUserName());
              assertNotNull(entity.getCreateTime());

              return 1;
            });

    // When
    ResponseDTO<String> response = chainManager.add(form, TEST_USER_ID, TEST_USER_NAME);

    // Then
    assertTrue(response.getOk());
    verify(chainDao, times(1)).insert(any(LiteFlowChainEntity.class));
  }
}
