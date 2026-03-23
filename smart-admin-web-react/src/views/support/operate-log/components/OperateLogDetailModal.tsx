/**
 * Operate Log Detail Modal
 * 操作日誌詳情 Modal
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-13
 */

import React, { forwardRef, useImperativeHandle, useState } from 'react';
import { Modal, Row, Col, Typography, Card, Spin } from 'antd';
import { UAParser } from 'ua-parser-js';
import { operateLogApi } from '@/api/support/operateLogApi';
import { formatDateTime } from '@/utils/date';
import type { OperateLogVO } from '../types';

/**
 * JSON 顯示組件
 */
const JsonViewer: React.FC<{ value: any }> = ({ value }) => {
  if (!value) return <div>-</div>;

  try {
    const jsonString = typeof value === 'string' ? value : JSON.stringify(value, null, 2);
    return (
      <Card bodyStyle={{ padding: '12px', backgroundColor: '#f5f5f5' }}>
        <pre style={{ margin: 0, fontSize: '12px', maxHeight: '400px', overflow: 'auto' }}>
          {jsonString}
        </pre>
      </Card>
    );
  } catch {
    return <div>{String(value)}</div>;
  }
};

export interface OperateLogDetailModalRef {
  show: (id: number) => void;
}

const OperateLogDetailModal = forwardRef<OperateLogDetailModalRef>((_, ref) => {
  const [visible, setVisible] = useState(false);
  const [loading, setLoading] = useState(false);
  const [detail, setDetail] = useState<OperateLogVO | null>(null);

  // 暴露 show 方法
  useImperativeHandle(ref, () => ({
    show: async (operateLogId: number) => {
      setVisible(true);
      setDetail(null);
      setLoading(true);

      try {
        const res = await operateLogApi.detail(operateLogId);
        const data = res.data;

        // 解析 UserAgent
        if (data.userAgent) {
          const parser = UAParser(data.userAgent);
          const browser = parser.browser;
          const os = parser.os;
          const device = parser.device;

          data.browser = browser?.name || '';
          data.os = os?.name || '';
          data.device = device?.vendor && device?.model ? `${device.vendor} ${device.model}` : '';
        }

        setDetail(data);
      } catch (error) {
        console.error('Failed to fetch operate log detail:', error);
      } finally {
        setLoading(false);
      }
    },
  }));

  const handleClose = () => {
    setVisible(false);
  };

  return (
    <Modal
      open={visible}
      title="請求詳情"
      width="60%"
      footer={null}
      onCancel={handleClose}
      destroyOnClose
    >
      <Spin spinning={loading}>
        {detail && (
          <div>
            {/* 基本信息 */}
            <div style={{ padding: '10px 8px' }}>
              <Row style={{ marginTop: 10 }}>
                <Col span={16}>
                  <Row style={{ marginBottom: 12 }}>
                    <Col span={12}>用戶id：{detail.operateUserId}</Col>
                    <Col span={12}>用戶名稱：{detail.operateUserName}</Col>
                  </Row>
                  <Row style={{ marginBottom: 12 }}>
                    <Col span={12}>請求url：{detail.url}</Col>
                    <Col span={12}>請求日期：{formatDateTime(detail.createTime || '')}</Col>
                  </Row>
                  <Row style={{ marginBottom: 12 }}>
                    <Col span={12}>請求IP：{detail.ip}</Col>
                    <Col span={12}>IP地區：{detail.ipRegion || '-'}</Col>
                  </Row>
                  <Row style={{ marginBottom: 12 }}>
                    <Col span={24}>
                      客戶端：
                      {[detail.os, detail.browser, detail.device].filter(Boolean).join(' / ') ||
                        '-'}
                    </Col>
                  </Row>
                </Col>
                <Col span={8}>
                  <p style={{ textAlign: 'right', color: 'grey' }}>請求狀態</p>
                  <Typography.Text
                    style={{
                      paddingLeft: 5,
                      fontSize: 20,
                      fontWeight: 'bold',
                      textAlign: 'right',
                      float: 'right',
                    }}
                    type={detail.successFlag === 1 ? 'success' : 'danger'}
                  >
                    {detail.successFlag === 1 ? '成功' : '失敗'}
                  </Typography.Text>
                </Col>
              </Row>
              <Row style={{ marginBottom: 12 }}>
                <Col span={24}>方法：{detail.method}</Col>
              </Row>
              <Row style={{ marginBottom: 12 }}>
                <Col span={24}>
                  說明：{detail.module} - {detail.content}
                </Col>
              </Row>
            </div>

            {/* 請求參數 */}
            <div style={{ padding: '10px 8px' }}>
              <h4>請求參數：</h4>
              <JsonViewer value={detail.param} />
            </div>

            {/* 返回結果 */}
            {detail.successFlag === 1 && (
              <div style={{ padding: '10px 8px' }}>
                <h4>返回結果：</h4>
                <JsonViewer value={detail.response} />
              </div>
            )}

            {/* 請求失敗原因 */}
            {detail.failReason && (
              <div style={{ padding: '10px 8px' }}>
                <h4>請求失敗原因：</h4>
                <Card>{detail.failReason}</Card>
              </div>
            )}
          </div>
        )}
      </Spin>
    </Modal>
  );
});

OperateLogDetailModal.displayName = 'OperateLogDetailModal';

export default OperateLogDetailModal;
