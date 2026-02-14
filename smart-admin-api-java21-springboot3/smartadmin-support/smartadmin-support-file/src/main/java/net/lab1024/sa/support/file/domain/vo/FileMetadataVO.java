package net.lab1024.sa.support.file.domain.vo;

import lombok.Data;

/**
 * 文件元数据
 *
 * @author 1024创新实验室: 罗伊
 * @since 2019年10月11日 15:34:47 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
public class FileMetadataVO {

  /** 文件名称 */
  private String fileName;

  /** 文件大小/字节 */
  private Long fileSize;

  /** 文件格式 */
  private String fileFormat;
}
