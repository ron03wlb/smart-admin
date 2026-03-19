/**
 * Code Generator Preview Modal - 代碼預覽模態框
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/code-generator/components/preview/code-generator-preview-modal.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { useState, useImperativeHandle, forwardRef } from 'react';
import { Modal, Radio, Tabs, Button, Row, message } from 'antd';
import type { TableInfo } from '@/api/support/codeGeneratorApi';
import { codeGeneratorApi } from '@/api/support/codeGeneratorApi';
import {
  LANGUAGE_LIST,
  JS_FILE_LIST,
  TS_FILE_LIST,
  JAVA_FILE_LIST,
} from '../utils/codeGeneratorUtils';
import './PreviewModal.css';

export interface PreviewModalRef {
  showModal: (table: TableInfo) => void;
}

const PreviewModal = forwardRef<PreviewModalRef>((_props, ref) => {
  const [open, setOpen] = useState(false);
  const [languageType, setLanguageType] = useState(LANGUAGE_LIST[0]);
  const [activeFileKey, setActiveFileKey] = useState(JS_FILE_LIST[0]);
  const [codeContent, setCodeContent] = useState('');
  const [loading, setLoading] = useState(false);
  const [currentTable, setCurrentTable] = useState<TableInfo | null>(null);

  /**
   * 顯示 Modal
   */
  const showModal = (table: TableInfo) => {
    setCurrentTable(table);
    setLanguageType(LANGUAGE_LIST[0]);
    setActiveFileKey(JS_FILE_LIST[0]);
    setOpen(true);

    // 加載第一個文件的代碼
    loadCode(table.tableName, JS_FILE_LIST[0]);
  };

  /**
   * 關閉 Modal
   */
  const handleClose = () => {
    setOpen(false);
    setCodeContent('');
  };

  /**
   * 加載代碼
   */
  const loadCode = async (tableName: string, templateFile: string) => {
    try {
      setLoading(true);
      const result = await codeGeneratorApi.preview({
        tableName,
        templateFile,
      });
      setCodeContent(result.data || '');
    } catch (error) {
      message.error('加載代碼失敗');
      console.error('Load code error:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 語言類型變化
   */
  const handleLanguageChange = (e: any) => {
    const newLanguage = e.target.value;
    setLanguageType(newLanguage);

    let newFileKey: string;
    if (newLanguage === LANGUAGE_LIST[0]) {
      newFileKey = JS_FILE_LIST[0];
    } else if (newLanguage === LANGUAGE_LIST[1]) {
      newFileKey = TS_FILE_LIST[0];
    } else {
      newFileKey = JAVA_FILE_LIST[0];
    }

    setActiveFileKey(newFileKey);
    if (currentTable) {
      loadCode(currentTable.tableName, newFileKey);
    }
  };

  /**
   * 文件 Tab 變化
   */
  const handleFileTabChange = (key: string) => {
    setActiveFileKey(key);
    if (currentTable) {
      loadCode(currentTable.tableName, key);
    }
  };

  /**
   * 下載代碼
   */
  const handleDownload = () => {
    if (currentTable) {
      codeGeneratorApi.downloadCode(currentTable.tableName);
      message.success('下載請求已發送');
    }
  };

  /**
   * 複製代碼
   */
  const handleCopy = () => {
    navigator.clipboard.writeText(codeContent).then(() => {
      message.success('複製成功');
    }).catch(() => {
      message.error('複製失敗');
    });
  };

  // 暴露方法給父組件
  useImperativeHandle(ref, () => ({
    showModal,
  }));

  /**
   * 獲取當前文件列表
   */
  const getFileList = () => {
    if (languageType === LANGUAGE_LIST[0]) {
      return JS_FILE_LIST;
    } else if (languageType === LANGUAGE_LIST[1]) {
      return TS_FILE_LIST;
    } else {
      return JAVA_FILE_LIST;
    }
  };

  /**
   * Tabs 配置
   */
  const fileTabs = getFileList().map((file) => ({
    key: file,
    label: file,
  }));

  return (
    <Modal
      title="代碼預覽"
      open={open}
      width={1200}
      onCancel={handleClose}
      footer={null}
      destroyOnClose
    >
      {/* 語言選擇和下載按鈕 */}
      <Row justify="space-between" style={{ marginBottom: 16 }}>
        <Radio.Group
          value={languageType}
          onChange={handleLanguageChange}
          buttonStyle="solid"
        >
          <Radio.Button value={LANGUAGE_LIST[0]}>JavaScript代碼</Radio.Button>
          <Radio.Button value={LANGUAGE_LIST[1]}>TypeScript代碼</Radio.Button>
          <Radio.Button value={LANGUAGE_LIST[2]}>Java代碼</Radio.Button>
        </Radio.Group>

        <div>
          <Button size="small" onClick={handleCopy} style={{ marginRight: 8 }}>
            複製代碼
          </Button>
          <Button type="link" onClick={handleDownload} danger size="small">
            <strong>下載代碼</strong>
          </Button>
        </div>
      </Row>

      {/* 文件 Tabs */}
      <Tabs
        activeKey={activeFileKey}
        onChange={handleFileTabChange}
        size="small"
        items={fileTabs}
      />

      {/* 代碼內容 */}
      <div className="code-preview-container">
        <pre className="code-content">{loading ? '加載中...' : codeContent}</pre>
      </div>
    </Modal>
  );
});

PreviewModal.displayName = 'PreviewModal';

export default PreviewModal;
