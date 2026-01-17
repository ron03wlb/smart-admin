package net.lab1024.sa.base.module.support.file.domain.vo;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;

/**
 * 文件下载
 *
 * @author 1024创新实验室: 罗伊
 * @since 2019年10月11日 15:34:47 Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Data
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class FileDownloadVO {

  /** 文件字节数据 */
  private byte[] data;

  /** 文件元数据 */
  private FileMetadataVO metadata;
}
