package net.lab1024.sa.base.module.support.file.service;

import net.lab1024.sa.base.module.support.file.domain.vo.FileDownloadVO;
import net.lab1024.sa.base.module.support.file.domain.vo.FileUploadVO;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 接口
 *
 * @author 1024创新实验室: 罗伊
 * @since 2019年10月11日 15:34:47 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
public interface IFileStorageService {

  /**
   * 文件上传
   *
   * @param file
   * @param path
   * @return
   */
  ResponseDTO<FileUploadVO> upload(MultipartFile file, String path);

  /**
   * 获取文件url
   *
   * @param fileKey
   * @return
   */
  ResponseDTO<String> getFileUrl(String fileKey);

  /**
   * 流式下载（名称为原文件）
   *
   * @param key
   * @return
   */
  ResponseDTO<FileDownloadVO> download(String key);

  /**
   * 单个删除文件 根据文件key删除
   *
   * @param fileKey
   * @return
   */
  ResponseDTO<String> delete(String fileKey);

  /** Static map for file extension to content type mapping */
  java.util.Map<String, String> CONTENT_TYPE_MAP =
      java.util.Map.ofEntries(
          java.util.Map.entry("bmp", "image/bmp"),
          java.util.Map.entry("gif", "image/gif"),
          java.util.Map.entry("jpeg", "image/jpeg"),
          java.util.Map.entry("jpg", "image/jpeg"),
          java.util.Map.entry("png", "image/png"),
          java.util.Map.entry("html", "text/html"),
          java.util.Map.entry("txt", "text/plain"),
          java.util.Map.entry("vsd", "application/vnd.visio"),
          java.util.Map.entry("ppt", "application/vnd.ms-powerpoint"),
          java.util.Map.entry("pptx", "application/vnd.ms-powerpoint"),
          java.util.Map.entry("doc", "application/msword"),
          java.util.Map.entry("docx", "application/msword"),
          java.util.Map.entry("pdf", "application/pdf"),
          java.util.Map.entry("xml", "text/xml"));

  /**
   * 获取文件类型
   *
   * @param fileExt
   * @return
   */
  default String getContentType(String fileExt) {
    if (fileExt == null) {
      return "";
    }
    return CONTENT_TYPE_MAP.getOrDefault(fileExt.toLowerCase(java.util.Locale.ROOT), "");
  }
}
