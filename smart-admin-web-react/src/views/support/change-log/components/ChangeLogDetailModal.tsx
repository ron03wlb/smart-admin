/**
 * ChangeLog Detail Modal
 * 系統更新日誌詳情 Modal
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/change-log/change-log-modal.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-17
 */

import { forwardRef, useImperativeHandle, useState } from 'react';
import { Modal } from 'antd';

import type { ChangeLogVO } from '../types';

interface ChangeLogDetailModalProps {}

const ChangeLogDetailModal = forwardRef<
  { show: (record: ChangeLogVO) => void },
  ChangeLogDetailModalProps
>((_props, ref) => {
  const [visible, setVisible] = useState(false);
  const [content, setContent] = useState('');
  const [link, setLink] = useState<string | undefined>();

  /**
   * 顯示 Modal
   */
  useImperativeHandle(ref, () => ({
    show: (record: ChangeLogVO) => {
      setContent(record.content || '');
      setLink(record.link);
      setVisible(true);
    },
  }));

  /**
   * 關閉 Modal
   */
  const handleClose = () => {
    setVisible(false);
    setContent('');
    setLink(undefined);
  };

  return (
    <Modal
      title="更新日誌"
      open={visible}
      onCancel={handleClose}
      footer={null}
      width={700}
      destroyOnClose
    >
      <div>
        <pre style={{ whiteSpace: 'pre-wrap', wordWrap: 'break-word' }}>{content}</pre>
        {link && (
          <div style={{ marginTop: 16 }}>
            鏈接：
            <a href={link} target="_blank" rel="noreferrer">
              {link}
            </a>
          </div>
        )}
      </div>
    </Modal>
  );
});

ChangeLogDetailModal.displayName = 'ChangeLogDetailModal';

export default ChangeLogDetailModal;
