/**
 * File Upload Component
 *
 * Corresponds to Vue's components/support/file-upload/index.vue
 * Supports image preview and file download with Authorization header.
 */
import React, { useState } from 'react';
import { Upload, message, Image } from 'antd';
import { PlusOutlined, UploadOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd';
import { LocalStorageKey, localRead } from '@/utils/local-storage';

const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'];

function isImage(fileName: string): boolean {
  const ext = fileName.split('.').pop()?.toLowerCase() ?? '';
  return IMAGE_EXTENSIONS.includes(ext);
}

interface FileUploadProps {
  value?: UploadFile[];
  onChange?: (fileList: UploadFile[]) => void;
  /** Upload folder type */
  folder?: number;
  /** Max file count */
  maxCount?: number;
  /** Max file size in MB */
  maxSize?: number;
  /** Accepted file types (e.g., ".jpg,.png,.pdf") */
  accept?: string;
  /** Display mode */
  listType?: 'text' | 'picture-card';
  disabled?: boolean;
  /** Upload API URL */
  action?: string;
}

const FileUpload: React.FC<FileUploadProps> = ({
  value = [],
  onChange,
  folder = 1,
  maxCount = 5,
  maxSize = 10,
  accept,
  listType = 'picture-card',
  disabled = false,
  action,
}) => {
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewImage, setPreviewImage] = useState('');

  const uploadAction = action || `${import.meta.env.VITE_APP_API_URL || '/api'}/support/file/upload`;

  const handlePreview = async (file: UploadFile) => {
    if (file.url && isImage(file.name || file.url)) {
      setPreviewImage(file.url);
      setPreviewOpen(true);
    } else if (file.url) {
      window.open(file.url, '_blank');
    }
  };

  const handleChange: UploadProps['onChange'] = ({ fileList }) => {
    onChange?.(fileList);
  };

  const beforeUpload = (file: File) => {
    const isLt = file.size / 1024 / 1024 < maxSize;
    if (!isLt) {
      message.error(`文件大小不能超过 ${maxSize}MB`);
      return Upload.LIST_IGNORE;
    }
    return true;
  };

  const customHeaders = (): Record<string, string> => {
    const token = localRead<string>(LocalStorageKey.USER_TOKEN);
    if (token) {
      return { Authorization: `Bearer ${token}` };
    }
    return {};
  };

  const uploadButton =
    listType === 'picture-card' ? (
      <div>
        <PlusOutlined />
        <div style={{ marginTop: 8 }}>上传</div>
      </div>
    ) : (
      <div>
        <UploadOutlined /> 上传文件
      </div>
    );

  return (
    <>
      <Upload
        action={uploadAction}
        listType={listType}
        fileList={value}
        onPreview={handlePreview}
        onChange={handleChange}
        beforeUpload={beforeUpload}
        headers={customHeaders()}
        data={{ folder }}
        maxCount={maxCount}
        accept={accept}
        disabled={disabled}
      >
        {value.length >= maxCount ? null : uploadButton}
      </Upload>
      {previewOpen && (
        <Image
          style={{ display: 'none' }}
          preview={{
            visible: previewOpen,
            onVisibleChange: (visible) => setPreviewOpen(visible),
          }}
          src={previewImage}
        />
      )}
    </>
  );
};

export default FileUpload;
