package net.lab1024.sa.foundation.securityprotect.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件安全检测服务
 *
 * <p>提供文件MIME类型检测和安全性验证功能，使用Apache Tika进行真实文件类型检测。
 *
 * @author 1024创新实验室-主任: 卓大
 * @since 2025-01-20 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
public class FileSecurityService {

  /** 白名单MIME类型列表 */
  private static final List<String> ALLOWED_MIME_TYPES =
      Arrays.asList(
          "application/json",
          "application/zip",
          "application/x-7z-compressed",
          "application/pdf",
          "application/vnd.ms-excel",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          "application/vnd.ms-powerpoint",
          "application/vnd.openxmlformats-officedocument.presentationml.presentation",
          "application/msword",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "application/vnd.ms-works",
          "text/csv",
          "text/plain",
          "audio/*",
          "video/*",
          // 图片类型 svg有安全隐患，所以不使用"image/*"
          "image/jpeg",
          "image/png",
          "image/gif",
          "image/bmp",
          "image/webp");

  /**
   * 检测文件类型是否安全
   *
   * @param file 待检测的文件
   * @return 检测失败时返回错误消息，检测通过返回 empty
   */
  public Optional<String> checkFileType(MultipartFile file) {
    String fileType = getFileMimeType(file);
    if (ALLOWED_MIME_TYPES.stream()
        .noneMatch(allowedType -> matchesMimeType(fileType, allowedType))) {
      return Optional.of("禁止上传此文件类型");
    }
    return Optional.empty();
  }

  /**
   * 检测文件大小
   *
   * @param file 待检测的文件
   * @param maxSizeMb 最大文件大小（MB）
   * @return 检测失败时返回错误消息，检测通过返回 empty
   */
  public Optional<String> checkFileSize(MultipartFile file, long maxSizeMb) {
    if (maxSizeMb <= 0) {
      return Optional.empty();
    }

    long maxSize = maxSizeMb * 1024 * 1024;
    if (file.getSize() > maxSize) {
      return Optional.of("上传文件最大为:" + maxSizeMb + " mb");
    }
    return Optional.empty();
  }

  /**
   * 获取文件的 MIME 类型
   *
   * <p>使用 Apache Tika 进行真实文件类型检测，不依赖文件扩展名。
   *
   * @param file 要检查的文件
   * @return 文件的 MIME 类型
   */
  public String getFileMimeType(MultipartFile file) {
    try (InputStream inputStream = file.getInputStream();
        TikaInputStream stream = TikaInputStream.get(inputStream)) {
      TikaConfig tika = new TikaConfig();
      Metadata metadata = new Metadata();
      metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, file.getOriginalFilename());
      MediaType mimetype = tika.getDetector().detect(stream, metadata);
      return mimetype.toString();
    } catch (IOException | TikaException e) {
      if (log.isErrorEnabled()) {
        log.error("获取文件MIME类型失败: {}", e.getMessage(), e);
      }
      return MimeTypes.OCTET_STREAM;
    }
  }

  /**
   * 检查文件的 MIME 类型是否与指定的MIME 类型匹配（支持通配符）
   *
   * @param fileType 文件的 MIME 类型
   * @param mimetype MIME 类型（支持通配符，如 image/*）
   * @return 是否匹配
   */
  private boolean matchesMimeType(String fileType, String mimetype) {
    if (mimetype.endsWith("/*")) {
      String prefix = mimetype.substring(0, mimetype.length() - 1);
      return fileType.startsWith(prefix);
    } else {
      return fileType.equalsIgnoreCase(mimetype);
    }
  }
}
