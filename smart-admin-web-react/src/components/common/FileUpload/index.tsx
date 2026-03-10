/**
 * FileUpload Component
 * 文件上傳組件
 *
 * 參考：Vue 版本 smart-admin-web/src/components/support/file-upload/index.vue
 *
 * 用法：
 * <FileUpload
 *   folder={FILE_FOLDER_TYPE_ENUM.COMMON.value}
 *   maxUploadSize={5}
 *   maxSize={10}
 *   accept=".jpg,.png"
 *   multiple
 *   onChange={(fileList) => console.log(fileList)}
 * />
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-10
 */

import React, { useState, useCallback, useImperativeHandle, forwardRef, useMemo } from 'react';
import { Upload, Modal, Button, message } from 'antd';
import { PlusOutlined, UploadOutlined } from '@ant-design/icons';
import type { UploadFile, UploadProps } from 'antd';
import { fileApi, FileUploadResponse } from '@/api/support/fileApi';
import { SmartLoading } from '@/utils/SmartLoading';
import { FILE_FOLDER_TYPE_ENUM } from '@/constants/support/fileConst';
import { useAppSelector } from '@/hooks/useRedux';
import { selectToken } from '@/store/slices/userSlice';
import './index.css';

/**
 * 文件上傳組件 Props
 */
export interface FileUploadProps {
  /** 上傳按鈕文字 */
  buttonText?: string;
  /** 默認文件列表 */
  defaultFileList?: FileUploadResponse[];
  /** 是否支持多選文件 */
  multiple?: boolean;
  /** 最多上傳文件數量 */
  maxUploadSize?: number;
  /** 單個文件最大尺寸（MB） */
  maxSize?: number;
  /** 接受的文件類型（如 ".jpg,.png"） */
  accept?: string;
  /** 文件上傳文件夾類型 */
  folder?: number;
  /** 上傳列表的內建樣式 */
  listType?: 'text' | 'picture' | 'picture-card';
  /** 文件列表變化回調 */
  onChange?: (fileList: FileUploadResponse[]) => void;
}

/**
 * 文件上傳組件 Ref
 */
export interface FileUploadRef {
  /** 清空文件列表 */
  clear: () => void;
}

/**
 * 圖片類型的後綴名
 */
const IMG_FILE_TYPES = ['jpg', 'jpeg', 'png', 'gif'];

/**
 * 文件上傳組件
 */
const FileUpload = forwardRef<FileUploadRef, FileUploadProps>((props, ref) => {
  const {
    buttonText = '點擊上傳附件',
    defaultFileList = [],
    multiple = false,
    maxUploadSize = 10,
    maxSize = 10,
    accept = '',
    folder = FILE_FOLDER_TYPE_ENUM.COMMON.value,
    listType = 'picture-card',
    onChange,
  } = props;

  const token = useAppSelector(selectToken);

  // 文件列表狀態
  const [fileList, setFileList] = useState<UploadFile[]>(() => {
    return defaultFileList.map(file => ({
      uid: String(file.fileId),
      name: file.fileName,
      url: file.fileUrl,
      status: 'done' as const,
      response: file,
    }));
  });

  // 預覽狀態
  const [previewVisible, setPreviewVisible] = useState(false);
  const [previewUrl, setPreviewUrl] = useState('');

  // 錯誤彈窗標誌（防止重複彈窗）
  const [showErrorModalFlag, setShowErrorModalFlag] = useState(true);

  /**
   * 顯示錯誤提示（只顯示一次）
   */
  const showErrorMsgOnce = useCallback((content: string) => {
    if (showErrorModalFlag) {
      Modal.error({
        title: '提示',
        content,
        okType: 'danger',
        centered: true,
        onOk() {
          setShowErrorModalFlag(true);
        },
      });
      setShowErrorModalFlag(false);
    }
  }, [showErrorModalFlag]);

  /**
   * 上傳前校驗
   */
  const beforeUpload: UploadProps['beforeUpload'] = useCallback(
    (file, files) => {
      // 檢查文件數量
      if (fileList.length + files.length > maxUploadSize) {
        showErrorMsgOnce(`最多支持上傳 ${maxUploadSize} 個文件哦！`);
        return false;
      }

      // 檢查文件類型
      if (accept) {
        const suffixIndex = file.name.lastIndexOf('.');
        const fileSuffix = file.name.substring(suffixIndex <= -1 ? 0 : suffixIndex);
        if (accept.indexOf(fileSuffix) === -1) {
          showErrorMsgOnce(`只支持上傳 ${accept.replaceAll(',', ' ')} 格式的文件`);
          return false;
        }
      }

      // 檢查文件大小
      const isLimitSize = file.size / 1024 / 1024 < maxSize;
      if (!isLimitSize) {
        showErrorMsgOnce(`單個文件大小必須小於 ${maxSize} MB`);
      }
      return isLimitSize;
    },
    [fileList.length, maxUploadSize, accept, maxSize, showErrorMsgOnce]
  );

  /**
   * 自定義上傳請求
   */
  const customRequest: UploadProps['customRequest'] = useCallback(
    async options => {
      SmartLoading.show();
      try {
        const formData = new FormData();
        formData.append('file', options.file);

        const response = await fileApi.uploadFile(formData, folder);
        if (response.ok && response.data) {
          const file = response.data;

          // 更新文件列表
          const newFile: UploadFile = {
            uid: String(file.fileId),
            name: file.fileName,
            url: file.fileUrl,
            status: 'done',
            response: file,
          };

          setFileList(prev => {
            const newList = [...prev, newFile];
            // 觸發 onChange 回調
            if (onChange) {
              onChange(newList.map(f => f.response as FileUploadResponse));
            }
            return newList;
          });

          message.success('文件上傳成功');

          // 調用 Ant Design Upload 的成功回調
          options.onSuccess?.(file);
        } else {
          message.error('文件上傳失敗');
          options.onError?.(new Error('Upload failed'));
        }
      } catch (error) {
        console.error('文件上傳異常:', error);
        message.error('文件上傳異常，請稍後重試');
        options.onError?.(error as Error);
      } finally {
        SmartLoading.hide();
      }
    },
    [folder, onChange]
  );

  /**
   * 文件列表變化處理
   */
  const handleChange: UploadProps['onChange'] = useCallback(
    info => {
      // 只處理移除操作（上傳操作在 customRequest 中處理）
      if (info.file.status === 'removed') {
        setFileList(prev => {
          const newList = prev.filter(f => f.uid !== info.file.uid);
          // 觸發 onChange 回調
          if (onChange) {
            onChange(newList.map(f => f.response as FileUploadResponse).filter(Boolean));
          }
          return newList;
        });
      }
    },
    [onChange]
  );

  /**
   * 預覽處理
   */
  const handlePreview: UploadProps['onPreview'] = useCallback(async file => {
    const response = file.response as FileUploadResponse | undefined;
    if (!response) {
      return;
    }

    // 判斷是否為圖片
    if (IMG_FILE_TYPES.some(type => type === response.fileType)) {
      setPreviewUrl(file.url || '');
      setPreviewVisible(true);
    } else {
      // 非圖片類型，下載文件
      fileApi.downLoadFile(response.fileKey);
    }
  }, []);

  /**
   * 關閉預覽
   */
  const handleCancel = useCallback(() => {
    setPreviewVisible(false);
  }, []);

  /**
   * 清空文件列表
   */
  const clear = useCallback(() => {
    setFileList([]);
    if (onChange) {
      onChange([]);
    }
  }, [onChange]);

  // 暴露清空方法
  useImperativeHandle(ref, () => ({
    clear,
  }));

  /**
   * 上傳請求頭
   */
  const headers = useMemo(
    () => ({
      Authorization: `Bearer ${token}`,
    }),
    [token]
  );

  /**
   * 是否顯示上傳按鈕
   */
  const showUploadButton = fileList.length < maxUploadSize;

  return (
    <div className="file-upload-container">
      <Upload
        multiple={multiple}
        accept={accept}
        beforeUpload={beforeUpload}
        customRequest={customRequest}
        fileList={fileList}
        headers={headers}
        listType={listType}
        onChange={handleChange}
        onPreview={handlePreview}
      >
        {showUploadButton && (
          <>
            {listType === 'picture-card' && (
              <div>
                <PlusOutlined />
                <div className="ant-upload-text">{buttonText}</div>
              </div>
            )}
            {listType === 'text' && (
              <Button icon={<UploadOutlined />}>{buttonText}</Button>
            )}
          </>
        )}
      </Upload>

      <Modal open={previewVisible} footer={null} onCancel={handleCancel}>
        <img src={previewUrl} alt="preview" style={{ width: '100%' }} />
      </Modal>
    </div>
  );
});

FileUpload.displayName = 'FileUpload';

export default FileUpload;
