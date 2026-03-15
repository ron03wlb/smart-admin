/**
 * Serial Number List Page
 * 單號生成器列表頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import React, { useEffect, useRef, useState } from 'react';
import { Card, Alert, Table, Button, Space } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { serialNumberApi } from '@/api/support/serialNumberApi';
import {
  SERIAL_NUMBER_PERMISSION,
  SERIAL_NUMBER_TABLE_COLUMNS_WIDTH,
} from '@/constants/support/serialNumberConst';
import { usePrivilege } from '@/hooks/usePrivilege';
import type { SerialNumberVO } from './types';
import SerialNumberGenerateModal from './components/SerialNumberGenerateModal';
import SerialNumberRecordModal from './components/SerialNumberRecordModal';

/**
 * 單號生成器列表組件
 */
const SerialNumberList: React.FC = () => {
  const hasGeneratePermission = usePrivilege(SERIAL_NUMBER_PERMISSION.GENERATE);
  const hasRecordPermission = usePrivilege(SERIAL_NUMBER_PERMISSION.RECORD);

  // 表格數據
  const [tableData, setTableData] = useState<SerialNumberVO[]>([]);
  const [tableLoading, setTableLoading] = useState(false);

  // Modal refs
  const generateModalRef = useRef<{ show: (record: SerialNumberVO) => void }>(null);
  const recordModalRef = useRef<{ show: (serialNumberId: number) => void }>(null);

  // 查詢數據
  const fetchData = async () => {
    try {
      setTableLoading(true);
      const res = await serialNumberApi.getAll();
      setTableData(res.data);
    } catch (error) {
      console.error('Failed to fetch serial number list:', error);
    } finally {
      setTableLoading(false);
    }
  };

  // 初始化加載數據
  useEffect(() => {
    fetchData();
  }, []);

  // 處理生成
  const handleGenerate = (record: SerialNumberVO) => {
    generateModalRef.current?.show(record);
  };

  // 處理查看記錄
  const handleShowRecord = (serialNumberId: number) => {
    recordModalRef.current?.show(serialNumberId);
  };

  // 表格列定義
  const columns: ColumnsType<SerialNumberVO> = [
    {
      title: 'ID',
      dataIndex: 'serialNumberId',
      key: 'serialNumberId',
      width: SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.serialNumberId,
    },
    {
      title: '業務',
      dataIndex: 'businessName',
      key: 'businessName',
      width: SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.businessName,
    },
    {
      title: '格式',
      dataIndex: 'format',
      key: 'format',
      width: SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.format,
    },
    {
      title: '循環週期',
      dataIndex: 'ruleType',
      key: 'ruleType',
      width: SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.ruleType,
    },
    {
      title: '初始值',
      dataIndex: 'initNumber',
      key: 'initNumber',
      width: SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.initNumber,
    },
    {
      title: '隨機增量',
      dataIndex: 'stepRandomRange',
      key: 'stepRandomRange',
      width: SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.stepRandomRange,
    },
    {
      title: '備註',
      dataIndex: 'remark',
      key: 'remark',
      width: SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.remark,
      ellipsis: true,
    },
    {
      title: '上次產生單號',
      dataIndex: 'lastNumber',
      key: 'lastNumber',
      width: SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.lastNumber,
    },
    {
      title: '上次產生時間',
      dataIndex: 'lastTime',
      key: 'lastTime',
      width: SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.lastTime,
    },
    {
      title: '操作',
      dataIndex: 'action',
      key: 'action',
      fixed: 'right',
      width: SERIAL_NUMBER_TABLE_COLUMNS_WIDTH.action,
      render: (_: any, record: SerialNumberVO) => (
        <Space>
          <Button
            type="link"
            size="small"
            disabled={!hasGeneratePermission}
            onClick={() => handleGenerate(record)}
          >
            生成
          </Button>
          <Button
            type="link"
            size="small"
            disabled={!hasRecordPermission}
            onClick={() => handleShowRecord(record.serialNumberId)}
          >
            查看記錄
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div className="serial-number-list">
      <Card size="small" style={{ marginBottom: 16 }}>
        <Alert
          message={<h4>SerialNumber 單號生成器介紹：</h4>}
          description={
            <div>
              <pre style={{ marginBottom: 16 }}>
{`簡介：SerialNumber是一個可以根據不同的日期、規則生成一系列特別單號的功能，比如訂單號、合同號、採購單號等等。
原理：內部有三種實現方式： 1) 基於內存鎖實現 （不支持分布式和集群）；  2) 基於redis鎖實現 ；  3) 基於Mysql 鎖for update 實現
- 支持隨機生成和查詢生成記錄
- 支持動態配置`}
              </pre>
              <div style={{ color: 'red' }}>
                系統默認使用"內存鎖"類型，若修改，請將後端代碼 @Service 注解加到對應的實現類上：SerialNumberInternService、SerialNumberMysqlService、SerialNumberRedisService
              </div>
            </div>
          }
          type="info"
        />
      </Card>

      <Card size="small">
        <Table
          rowKey="serialNumberId"
          columns={columns}
          dataSource={tableData}
          loading={tableLoading}
          pagination={false}
          size="small"
          bordered
          scroll={{ x: 1500 }}
        />
      </Card>

      {/* 生成 Modal */}
      <SerialNumberGenerateModal ref={generateModalRef} onRefresh={fetchData} />

      {/* 記錄 Modal */}
      <SerialNumberRecordModal ref={recordModalRef} />
    </div>
  );
};

export default SerialNumberList;
