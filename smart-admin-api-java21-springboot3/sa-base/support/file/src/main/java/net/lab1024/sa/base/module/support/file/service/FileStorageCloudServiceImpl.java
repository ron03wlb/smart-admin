package net.lab1024.sa.base.module.support.file.service;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.util.IdUtil;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.lab1024.sa.base.module.support.file.config.FileConfig;
import net.lab1024.sa.base.module.support.file.constant.FileFolderTypeEnum;
import net.lab1024.sa.base.module.support.file.dao.FileDao;
import net.lab1024.sa.base.module.support.file.domain.vo.FileDownloadVO;
import net.lab1024.sa.base.module.support.file.domain.vo.FileMetadataVO;
import net.lab1024.sa.base.module.support.file.domain.vo.FileUploadVO;
import net.lab1024.sa.base.module.support.file.domain.vo.FileVO;
import net.lab1024.sa.foundation.cache.CacheService;
import net.lab1024.sa.foundation.cache.constant.CacheKeyConst;
import net.lab1024.sa.foundation.domain.code.SystemErrorCode;
import net.lab1024.sa.foundation.domain.response.ResponseDTO;
import net.lab1024.sa.util.SmartStringUtil;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * 云计算 实现
 *
 * @author 1024创新实验室: 罗伊
 * @since 2019年10月11日 15:34:47 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@RequiredArgsConstructor
public class FileStorageCloudServiceImpl implements IFileStorageService {

  /** 自定义元数据 文件名称 */
  private static final String USER_METADATA_FILE_NAME = "file-name";

  /** 自定义元数据 文件格式 */
  private static final String USER_METADATA_FILE_FORMAT = "file-format";

  /** 自定义元数据 文件大小 */
  private static final String USER_METADATA_FILE_SIZE = "file-size";

  private final S3Client s3Client;

  private final FileConfig cloudConfig;

  private final CacheService cacheService;

  private final FileDao fileDao;

  @Override
  public ResponseDTO<FileUploadVO> upload(MultipartFile file, String path) {
    // 设置文件 key
    String originalFileName = file.getOriginalFilename();
    if (SmartStringUtil.isEmpty(originalFileName)) {
      return ResponseDTO.userErrorParam("上传文件名为空");
    }

    String fileType = FilenameUtils.getExtension(originalFileName);
    String uuid = IdUtil.fastSimpleUUID();
    String time =
        LocalDateTimeUtil.format(LocalDateTime.now(), DatePattern.PURE_DATETIME_FORMATTER);
    String fileKey = path + uuid + "_" + time + "." + fileType;

    // 文件名称 URL 编码
    String urlEncoderFilename;
    urlEncoderFilename = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8);
    Map<String, String> userMetadata = new HashMap<>(10);
    userMetadata.put(USER_METADATA_FILE_NAME, urlEncoderFilename);
    userMetadata.put(USER_METADATA_FILE_FORMAT, fileType);
    userMetadata.put(USER_METADATA_FILE_SIZE, String.valueOf(file.getSize()));

    // 根据文件路径获取并设置访问权限
    ObjectCannedACL acl = this.getACL(path);
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(cloudConfig.getBucketName())
            .key(fileKey)
            .metadata(userMetadata)
            .contentLength(file.getSize())
            .contentType(this.getContentType(fileType))
            .contentEncoding(StandardCharsets.UTF_8.name())
            .contentDisposition("attachment;filename=" + urlEncoderFilename)
            .acl(acl)
            .build();
    try (InputStream inputStream = file.getInputStream()) {
      s3Client.putObject(
          putObjectRequest, RequestBody.fromInputStream(inputStream, file.getSize()));
    } catch (IOException e) {
      log.error("文件上传-IO异常: fileKey={}", fileKey, e);
      return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "文件读取失败");
    } catch (S3Exception e) {
      log.error("文件上传-S3异常: fileKey={}, errorCode={}", fileKey, e.awsErrorDetails().errorCode(), e);
      return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "文件存储失败");
    } catch (Exception e) {
      log.error("文件上传-未知异常: fileKey={}", fileKey, e);
      return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "上传失败");
    }
    // 返回上传结果
    FileUploadVO uploadVO = new FileUploadVO();
    uploadVO.setFileName(originalFileName);
    uploadVO.setFileType(fileType);
    // 根据 访问权限 返回不同的 URL
    String url = cloudConfig.getUrlPrefix() + fileKey;
    if (ObjectCannedACL.PRIVATE.equals(acl)) {
      // 获取临时访问的URL
      url = this.getFileUrl(fileKey).getData();
    }
    uploadVO.setFileUrl(url);
    uploadVO.setFileKey(fileKey);
    uploadVO.setFileSize(file.getSize());
    return ResponseDTO.ok(uploadVO);
  }

  /**
   * 获取文件url
   *
   * @param fileKey 文件key
   * @return url
   */
  @Override
  public ResponseDTO<String> getFileUrl(String fileKey) {
    if (StringUtils.isBlank(fileKey)) {
      return ResponseDTO.userErrorParam("文件不存在，key为空");
    }

    if (!fileKey.startsWith(FileFolderTypeEnum.FOLDER_PRIVATE)) {
      // 不是私有的 都公共读
      return ResponseDTO.ok(cloudConfig.getUrlPrefix() + fileKey);
    }

    // 如果是私有的，则规定时间内可以访问，超过规定时间，则连接失效

    Optional<FileVO> fileVOOpt =
        cacheService.get(CacheKeyConst.Support.FILE_PRIVATE, fileKey, FileVO.class);
    FileVO fileVO = fileVOOpt.orElse(null);
    if (fileVO == null) {
      fileVO = fileDao.getByFileKey(fileKey);
      if (fileVO == null) {
        return ResponseDTO.userErrorParam("文件不存在");
      }

      try {
        GetObjectRequest getUrlRequest =
            GetObjectRequest.builder().bucket(cloudConfig.getBucketName()).key(fileKey).build();
        GetObjectPresignRequest getObjectPresignRequest =
            GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(cloudConfig.getPrivateUrlExpireSeconds()))
                .getObjectRequest(getUrlRequest)
                .build();

        String url;
        // 构建 endpoint URI（支持 http/https）
        String endpointUrl =
            cloudConfig.getUrlPrefix().startsWith("https://")
                ? "https://" + cloudConfig.getEndpoint()
                : "http://" + cloudConfig.getEndpoint();

        try (S3Presigner presigner =
            S3Presigner.builder()
                .region(Region.of(cloudConfig.getRegion()))
                .endpointOverride(URI.create(endpointUrl))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                            cloudConfig.getAccessKey(), cloudConfig.getSecretKey())))
                .build()) {

          PresignedGetObjectRequest presignedGetObjectRequest =
              presigner.presignGetObject(getObjectPresignRequest);
          url = presignedGetObjectRequest.url().toString();
        }
        fileVO.setFileUrl(url);
        cacheService.put(
            CacheKeyConst.Support.FILE_PRIVATE,
            fileKey,
            fileVO,
            cloudConfig.getPrivateUrlExpireSeconds() - 5,
            TimeUnit.SECONDS);
      } catch (NoSuchKeyException e) {
        log.error("获取文件URL失败-文件不存在: fileKey={}", fileKey, e);
        return ResponseDTO.userErrorParam("文件不存在");
      } catch (S3Exception e) {
        log.error(
            "获取文件URL失败-S3异常: fileKey={}, errorCode={}",
            fileKey,
            e.awsErrorDetails().errorCode(),
            e);
        return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "获取文件访问链接失败");
      } catch (Exception e) {
        log.error("获取文件URL失败-未知异常: fileKey={}", fileKey, e);
        return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "获取文件访问链接失败");
      }
    }

    return ResponseDTO.ok(fileVO.getFileUrl());
  }

  /**
   * 流式下载（名称为原文件）
   *
   * <p>注意：此方法会将整个文件加载到内存中。对于大文件（>100MB），建议： 1. 使用 getFileUrl() 获取预签名 URL，让客户端直接从 MinIO 下载 2.
   * 或考虑实现分片下载机制
   *
   * @param key 文件key
   * @return 文件下载对象
   */
  @Override
  public ResponseDTO<FileDownloadVO> download(String key) {
    if (StringUtils.isBlank(key)) {
      return ResponseDTO.userErrorParam("文件key不能为空");
    }

    try {
      // 获取文件 meta
      HeadObjectRequest objectRequest =
          HeadObjectRequest.builder().bucket(this.cloudConfig.getBucketName()).key(key).build();
      HeadObjectResponse headObjectResponse = s3Client.headObject(objectRequest);
      Map<String, String> userMetadata = headObjectResponse.metadata();
      FileMetadataVO metadataDTO = null;
      if (MapUtils.isNotEmpty(userMetadata)) {
        metadataDTO = new FileMetadataVO();
        metadataDTO.setFileFormat(userMetadata.get(USER_METADATA_FILE_FORMAT));
        metadataDTO.setFileName(userMetadata.get(USER_METADATA_FILE_NAME));
        String fileSizeStr = userMetadata.get(USER_METADATA_FILE_SIZE);
        Long fileSize = StringUtils.isBlank(fileSizeStr) ? null : Long.valueOf(fileSizeStr);
        metadataDTO.setFileSize(fileSize);
      }

      // 获取对象内容 - 使用流式读取（更好的 API 实践）
      GetObjectRequest getObjectRequest =
          GetObjectRequest.builder().bucket(cloudConfig.getBucketName()).key(key).build();

      // 使用 ResponseTransformer.toBytes() 内部使用流式读取，比直接 toBytes 更高效
      ResponseBytes<GetObjectResponse> s3ClientObject =
          s3Client.getObject(getObjectRequest, ResponseTransformer.toBytes());

      byte[] buffer = s3ClientObject.asByteArray();
      FileDownloadVO fileDownloadVO = new FileDownloadVO();
      fileDownloadVO.setData(buffer);
      fileDownloadVO.setMetadata(metadataDTO);
      return ResponseDTO.ok(fileDownloadVO);
    } catch (NoSuchKeyException e) {
      log.error("文件下载失败-文件不存在: fileKey={}", key, e);
      return ResponseDTO.userErrorParam("文件不存在");
    } catch (S3Exception e) {
      log.error("文件下载失败-S3异常: fileKey={}, errorCode={}", key, e.awsErrorDetails().errorCode(), e);
      return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "文件下载失败");
    } catch (Exception e) {
      log.error("文件下载失败-未知异常: fileKey={}", key, e);
      return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "文件下载失败");
    }
  }

  /**
   * 根据文件夹路径 返回对应的访问权限
   *
   * @param fileKey 文件key
   * @return 权限
   */
  private ObjectCannedACL getACL(String fileKey) {
    // 公用读
    if (fileKey.contains(FileFolderTypeEnum.FOLDER_PUBLIC)) {
      return ObjectCannedACL.PUBLIC_READ;
    }
    // 其他默认私有读写
    return ObjectCannedACL.PRIVATE;
  }

  /**
   * 单个删除文件 根据 file key 删除文件 ps：不能删除fileKey不为空的文件夹
   *
   * <p>注意：此方法只删除 S3 对象和缓存，数据库记录需要在上层 Service 中处理
   *
   * @param fileKey 文件or文件夹
   * @return 删除结果
   */
  @Override
  public ResponseDTO<String> delete(String fileKey) {
    if (StringUtils.isBlank(fileKey)) {
      return ResponseDTO.userErrorParam("文件key不能为空");
    }

    try {
      // 删除 S3 对象
      DeleteObjectRequest deleteObjectRequest =
          DeleteObjectRequest.builder().bucket(cloudConfig.getBucketName()).key(fileKey).build();
      s3Client.deleteObject(deleteObjectRequest);

      // 清除缓存（如果是私有文件）
      if (fileKey.startsWith(FileFolderTypeEnum.FOLDER_PRIVATE)) {
        cacheService.remove(CacheKeyConst.Support.FILE_PRIVATE, fileKey);
        log.info("文件删除成功并清除缓存: fileKey={}", fileKey);
      } else {
        log.info("文件删除成功: fileKey={}", fileKey);
      }

      return ResponseDTO.ok();
    } catch (Exception e) {
      log.error("文件删除失败: fileKey={}", fileKey, e);
      return ResponseDTO.error(SystemErrorCode.SYSTEM_ERROR, "文件删除失败");
    }
  }
}
