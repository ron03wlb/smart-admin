package net.lab1024.sa.support.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.List;
import java.util.Set;
import net.lab1024.sa.common.core.domain.response.PageResult;
import net.lab1024.sa.common.core.domain.response.ResponseDTO;
import net.lab1024.sa.common.security.service.FileSecurityService;
import net.lab1024.sa.common.security.service.SecurityConfigProvider;
import net.lab1024.sa.support.file.dao.FileDao;
import net.lab1024.sa.support.file.domain.form.FileQueryForm;
import net.lab1024.sa.support.file.domain.vo.FileDownloadVO;
import net.lab1024.sa.support.file.domain.vo.FileMetadataVO;
import net.lab1024.sa.support.file.domain.vo.FileVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * FileService 單元測試
 *
 * <p>測試覆蓋：
 *
 * <ul>
 *   <li>getFileList - 批量獲取文件信息
 *   <li>getFileUrl - 獲取文件 URL
 *   <li>getDownloadFile - 下載文件
 *   <li>queryPage - 分頁查詢
 * </ul>
 *
 * @author SmartAdmin Team
 * @since 4.1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FileService 單元測試")
class FileServiceTest {

  @Mock private IFileStorageService fileStorageService;

  @Mock private FileDao fileDao;

  @Mock private FileSecurityService fileSecurityService;

  @Mock private SecurityConfigProvider securityConfigProvider;

  @InjectMocks private FileService fileService;

  // ==================== getFileList 測試 ====================

  @Nested
  @DisplayName("getFileList 批量獲取文件測試")
  class GetFileListTest {

    @Test
    @DisplayName("正常情況：應該返回文件列表")
    void shouldReturnFileList() {
      // Given
      List<String> fileKeyList = List.of("key1", "key2");
      FileVO file1 = createTestFileVO("key1");
      FileVO file2 = createTestFileVO("key2");

      when(fileDao.selectByFileKeyList(any(Set.class))).thenReturn(List.of(file1, file2));
      when(fileStorageService.getFileUrl("key1")).thenReturn(ResponseDTO.ok("http://url1"));
      when(fileStorageService.getFileUrl("key2")).thenReturn(ResponseDTO.ok("http://url2"));

      // When
      List<FileVO> result = fileService.getFileList(fileKeyList);

      // Then
      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("列表為空時：應該返回空列表")
    void shouldReturnEmptyListWhenEmpty() {
      // When
      List<FileVO> result = fileService.getFileList(List.of());

      // Then
      assertThat(result).isEmpty();
      verify(fileDao, never()).selectByFileKeyList(any());
    }

    @Test
    @DisplayName("列表為 null 時：應該返回空列表")
    void shouldReturnEmptyListWhenNull() {
      // When
      List<FileVO> result = fileService.getFileList(null);

      // Then
      assertThat(result).isEmpty();
    }
  }

  // ==================== getFileUrl 測試 ====================

  @Nested
  @DisplayName("getFileUrl 獲取文件 URL 測試")
  class GetFileUrlTest {

    @Test
    @DisplayName("正常情況：應該返回文件 URL")
    void shouldReturnFileUrl() {
      // Given
      String fileKey = "test-key";
      when(fileStorageService.getFileUrl(fileKey)).thenReturn(ResponseDTO.ok("http://test-url"));

      // When
      ResponseDTO<String> result = fileService.getFileUrl(fileKey);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).contains("http://test-url");
    }

    @Test
    @DisplayName("fileKey 為空時：應該返回錯誤")
    void shouldReturnErrorWhenKeyEmpty() {
      // When
      ResponseDTO<String> result = fileService.getFileUrl("");

      // Then
      assertThat(result.getOk()).isFalse();
    }

    @Test
    @DisplayName("多個 fileKey 用逗號分隔時：應該返回多個 URL")
    void shouldReturnMultipleUrls() {
      // Given
      String fileKeys = "key1,key2";
      when(fileStorageService.getFileUrl("key1")).thenReturn(ResponseDTO.ok("http://url1"));
      when(fileStorageService.getFileUrl("key2")).thenReturn(ResponseDTO.ok("http://url2"));

      // When
      ResponseDTO<String> result = fileService.getFileUrl(fileKeys);

      // Then
      assertThat(result.getOk()).isTrue();
      assertThat(result.getData()).contains("http://url1");
      assertThat(result.getData()).contains("http://url2");
    }
  }

  // ==================== getDownloadFile 測試 ====================

  @Nested
  @DisplayName("getDownloadFile 下載文件測試")
  class GetDownloadFileTest {

    @Test
    @DisplayName("正常情況：應該返回下載信息")
    void shouldReturnDownloadInfo() {
      // Given
      String fileKey = "test-key";
      FileVO fileVO = createTestFileVO(fileKey);
      FileDownloadVO downloadVO = new FileDownloadVO();
      FileMetadataVO metadata = new FileMetadataVO();
      downloadVO.setMetadata(metadata);

      when(fileDao.getByFileKey(fileKey)).thenReturn(fileVO);
      when(fileStorageService.download(fileKey)).thenReturn(ResponseDTO.ok(downloadVO));

      // When
      ResponseDTO<FileDownloadVO> result = fileService.getDownloadFile(fileKey, "Chrome");

      // Then
      assertThat(result.getOk()).isTrue();
    }

    @Test
    @DisplayName("文件不存在時：應該返回錯誤")
    void shouldReturnErrorWhenFileNotExists() {
      // Given
      String fileKey = "non-existent";
      when(fileDao.getByFileKey(fileKey)).thenReturn(null);

      // When
      ResponseDTO<FileDownloadVO> result = fileService.getDownloadFile(fileKey, "Chrome");

      // Then
      assertThat(result.getOk()).isFalse();
    }
  }

  // ==================== queryPage 測試 ====================

  @Nested
  @DisplayName("queryPage 分頁查詢測試")
  class QueryPageTest {

    @Test
    @DisplayName("正常情況：應該返回分頁結果")
    void shouldReturnPageResult() {
      // Given
      FileQueryForm form = new FileQueryForm();
      form.setPageNum(1L);
      form.setPageSize(10L);

      FileVO fileVO = createTestFileVO("test-key");
      when(fileDao.queryPage(any(Page.class), any(FileQueryForm.class)))
          .thenReturn(List.of(fileVO));

      // When
      PageResult<FileVO> result = fileService.queryPage(form);

      // Then
      assertThat(result).isNotNull();
      verify(fileDao).queryPage(any(Page.class), any(FileQueryForm.class));
    }
  }

  // ==================== Helper Methods ====================

  private FileVO createTestFileVO(String fileKey) {
    FileVO vo = new FileVO();
    vo.setFileId(1L);
    vo.setFileKey(fileKey);
    vo.setFileName("test.txt");
    vo.setFileSize(1024);
    return vo;
  }
}
