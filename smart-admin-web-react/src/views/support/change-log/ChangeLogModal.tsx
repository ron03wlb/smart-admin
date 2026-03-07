/**
 * Change Log Detail Modal
 *
 * Corresponds to Vue's support/change-log/change-log-modal.vue
 */
import React from 'react';
import { Modal } from 'antd';
import type { ChangeLogVO } from '@/api/support/change-log-api';

interface Props {
  open: boolean;
  changeLog?: ChangeLogVO;
  onClose: () => void;
}

const ChangeLogModal: React.FC<Props> = ({ open, changeLog, onClose }) => {
  return (
    <Modal title={`更新日志 - ${changeLog?.updateVersion || ''}`} open={open} onCancel={onClose} footer={null} width={700}>
      {changeLog && (
        <>
          <pre style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word', maxHeight: 500, overflow: 'auto', background: '#fafafa', padding: 16, borderRadius: 4 }}>
            {changeLog.content}
          </pre>
          {changeLog.link && (
            <div style={{ marginTop: 12 }}>
              <span>跳转链接：</span>
              <a href={changeLog.link} target="_blank" rel="noopener noreferrer">{changeLog.link}</a>
            </div>
          )}
        </>
      )}
    </Modal>
  );
};

export default ChangeLogModal;
