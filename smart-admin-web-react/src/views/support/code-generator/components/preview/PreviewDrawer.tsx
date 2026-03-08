/**
 * Code Preview Drawer
 *
 * Language tabs (JS/TS/Java), file tabs, syntax highlighting with highlight.js,
 * copy to clipboard, and download button.
 */
import { useState, useEffect, useImperativeHandle, forwardRef, useRef, useCallback } from 'react';
import { Drawer, Radio, Tabs, Button, Row, message, Spin } from 'antd';
import hljs from 'highlight.js/lib/core';
import javascript from 'highlight.js/lib/languages/javascript';
import typescript from 'highlight.js/lib/languages/typescript';
import java from 'highlight.js/lib/languages/java';
import xml from 'highlight.js/lib/languages/xml';
import 'highlight.js/styles/github-dark.css';
import { codeGeneratorApi } from '@/api/support/code-generator-api';
import { LANGUAGE_LIST, JS_FILE_LIST, TS_FILE_LIST, JAVA_FILE_LIST } from '../../code-generator-util';
import styles from './PreviewDrawer.module.css';

hljs.registerLanguage('javascript', javascript);
hljs.registerLanguage('typescript', typescript);
hljs.registerLanguage('java', java);
hljs.registerLanguage('xml', xml);

export interface PreviewDrawerRef {
  open: (tableInfo: { tableName: string; tableComment: string }) => void;
}

function getFileList(lang: string) {
  if (lang === LANGUAGE_LIST[0]) return JS_FILE_LIST;
  if (lang === LANGUAGE_LIST[1]) return TS_FILE_LIST;
  return JAVA_FILE_LIST;
}

function getHljsLanguage(lang: string) {
  if (lang === LANGUAGE_LIST[0]) return 'javascript';
  if (lang === LANGUAGE_LIST[1]) return 'typescript';
  if (lang === LANGUAGE_LIST[2]) {
    // JAVA_FILE_LIST includes Mapper.xml
    return 'java';
  }
  return 'javascript';
}

const PreviewDrawer = forwardRef<PreviewDrawerRef>((_props, ref) => {
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [tableName, setTableName] = useState('');
  const [languageType, setLanguageType] = useState(LANGUAGE_LIST[0]);
  const [fileKey, setFileKey] = useState(JS_FILE_LIST[0]);
  const [resultCode, setResultCode] = useState('');
  const codeRef = useRef<HTMLElement>(null);

  useImperativeHandle(ref, () => ({
    open: (tableInfo) => {
      setTableName(tableInfo.tableName);
      setLanguageType(LANGUAGE_LIST[0]);
      setFileKey(JS_FILE_LIST[0]);
      setVisible(true);
    },
  }));

  const fetchCode = useCallback(async (file: string, table: string) => {
    if (!table || !file) return;
    setLoading(true);
    try {
      const res = await codeGeneratorApi.preview({
        tableName: table,
        language: languageType,
        fileKey: file,
      });
      if (res.success) {
        setResultCode(res.data || '');
      }
    } catch {
      message.error('获取预览代码失败');
    } finally {
      setLoading(false);
    }
  }, [languageType]);

  // Fetch code when fileKey or tableName changes
  useEffect(() => {
    if (visible && tableName && fileKey) {
      fetchCode(fileKey, tableName);
    }
  }, [visible, tableName, fileKey, fetchCode]);

  // Apply syntax highlighting after code updates
  useEffect(() => {
    if (codeRef.current && resultCode) {
      codeRef.current.removeAttribute('data-highlighted');
      const lang = fileKey.endsWith('.xml') ? 'xml' : getHljsLanguage(languageType);
      codeRef.current.className = `language-${lang}`;
      hljs.highlightElement(codeRef.current);
    }
  }, [resultCode, languageType, fileKey]);

  const onChangeLanguage = (lang: string) => {
    setLanguageType(lang);
    const files = getFileList(lang);
    setFileKey(files[0]);
  };

  const onCopy = async () => {
    try {
      await navigator.clipboard.writeText(resultCode);
      message.success('复制成功！');
    } catch {
      message.error('复制失败');
    }
  };

  const onDownload = () => {
    codeGeneratorApi.downloadCode(tableName);
  };

  const fileList = getFileList(languageType);
  const tabItems = fileList.map((file) => ({ key: file, label: file }));

  return (
    <Drawer
      title="代码预览"
      open={visible}
      width={1200}
      onClose={() => setVisible(false)}
      maskClosable={false}
      destroyOnClose
      styles={{ body: { padding: '8px 24px' } }}
    >
      <Row justify="space-between" style={{ marginBottom: 10 }}>
        <Radio.Group value={languageType} buttonStyle="solid" onChange={(e) => onChangeLanguage(e.target.value)}>
          <Radio.Button value={LANGUAGE_LIST[0]}>JavaScript代码</Radio.Button>
          <Radio.Button value={LANGUAGE_LIST[1]}>TypeScript代码</Radio.Button>
          <Radio.Button value={LANGUAGE_LIST[2]}>Java代码</Radio.Button>
        </Radio.Group>
        <Button type="link" danger size="small" onClick={onDownload}><strong>下载代码</strong></Button>
      </Row>

      <Tabs activeKey={fileKey} size="small" onChange={setFileKey} items={tabItems} />

      <Spin spinning={loading}>
        <div className={styles.codeBlock}>
          <div className={styles.codeActions}>
            <Button size="small" onClick={onCopy}>复制代码</Button>
          </div>
          <pre><code ref={codeRef}>{resultCode}</code></pre>
        </div>
      </Spin>
    </Drawer>
  );
});

PreviewDrawer.displayName = 'PreviewDrawer';
export default PreviewDrawer;
