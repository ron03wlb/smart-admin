# MinIO API 使用示例

## 文件上传

### Controller 层调用

```java
@RestController
@RequestMapping("/business/document")
@RequiredArgsConstructor
public class DocumentController {
    
    private final IFileStorageService fileStorageService;
    
    @PostMapping("/upload")
    public ResponseDTO<FileUploadVO> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", defaultValue = "general") String category) {
        
        String folder = "documents/" + category + "/";
        return fileStorageService.upload(file, folder);
    }
    
    @PostMapping("/avatar/upload")
    public ResponseDTO<FileUploadVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return fileStorageService.upload(file, "public/avatars/");
    }
}
```

### HTTP 测试

```bash
curl -X POST http://localhost:1024/support/file/upload \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@test.jpg" \
  -F "folder=public/images/"
```

## 文件下载

```java
@GetMapping("/download")
public void downloadFile(
        @RequestParam("fileKey") String fileKey,
        HttpServletResponse response) {
    
    ResponseDTO<FileDownloadVO> result = fileStorageService.download(fileKey);
    
    if (result.isSuccess()) {
        FileDownloadVO fileDownload = result.getData();
        response.setContentType("application/octet-stream");
        // 写入响应流...
    }
}
```

## 公共 vs 私有文件

### 公共文件（public/ 前缀）

```java
// 上传到 public/ 目录
fileStorageService.upload(file, "public/images/");
// 返回的 URL 可以直接访问（无需认证）
```

### 私有文件（private/ 前缀）

```java
// 上传到 private/ 目录
fileStorageService.upload(file, "private/contracts/");
// 需要调用 getFileUrl() 获取预签名 URL
ResponseDTO<String> result = fileStorageService.getFileUrl(fileKey);
```

## 前端集成示例

### Vue 3 + Ant Design Vue

```vue
<template>
  <a-upload
    :action="uploadUrl"
    :headers="uploadHeaders"
    :data="uploadData"
    @change="handleChange"
  >
    <a-button>
      <upload-outlined /> 上传文件
    </a-button>
  </a-upload>
</template>

<script setup lang="ts">
const uploadUrl = ref('http://localhost:1024/support/file/upload');
const uploadHeaders = ref({
  Authorization: `Bearer ${localStorage.getItem('token')}`
});
const uploadData = ref({
  folder: 'documents/uploads/'
});
</script>
```

## 性能优化建议

1. 使用文件流而非字节数组
2. 启用 CDN 加速
3. 文件分片上传（大文件）

详见完整 API 使用文档。
