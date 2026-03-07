/**
 * Operate Log Detail Modal
 *
 * Corresponds to Vue's support/operate-log/components/operate-log-detail-modal.vue
 */
import React from 'react';
import { Modal, Descriptions, Tag } from 'antd';
import type { OperateLogVO } from '@/api/support/operate-log-api';

interface Props {
  open: boolean;
  log: OperateLogVO | null;
  onClose: () => void;
}

const OperateLogDetailModal: React.FC<Props> = ({ open, log, onClose }) => {
  if (!log) return null;

  return (
    <Modal title="操作日志详情" open={open} onCancel={onClose} footer={null} width={800}>
      <Descriptions bordered column={2} size="small">
        <Descriptions.Item label="操作人">{log.operateUserName}</Descriptions.Item>
        <Descriptions.Item label="模块">{log.module}</Descriptions.Item>
        <Descriptions.Item label="操作内容" span={2}>{log.content}</Descriptions.Item>
        <Descriptions.Item label="请求URL" span={2}>{log.url}</Descriptions.Item>
        <Descriptions.Item label="请求方式">{log.method}</Descriptions.Item>
        <Descriptions.Item label="IP">{log.ip} {log.ipRegion && `(${log.ipRegion})`}</Descriptions.Item>
        <Descriptions.Item label="结果">
          <Tag color={log.successFlag ? 'success' : 'error'}>{log.successFlag ? '成功' : '失败'}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="时间">{log.createTime}</Descriptions.Item>
        <Descriptions.Item label="UserAgent" span={2}>
          <div style={{ wordBreak: 'break-all' }}>{log.userAgent}</div>
        </Descriptions.Item>
        {log.failReason && (
          <Descriptions.Item label="失败原因" span={2}>
            <pre style={{ margin: 0, maxHeight: 200, overflow: 'auto', fontSize: 12 }}>{log.failReason}</pre>
          </Descriptions.Item>
        )}
      </Descriptions>
    </Modal>
  );
};

export default OperateLogDetailModal;
