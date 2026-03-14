/**
 * Data Masking Demo Page
 * 數據脫敏示例頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/level3protect/data-masking-list.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { useState, useEffect } from 'react';
import { Card, Alert, Form, Button, Table } from 'antd';
import { SearchOutlined } from '@ant-design/icons';
import { dataMaskingApi, type DataMaskingVO } from '@/api/support/dataMaskingApi';
import type { ColumnsType } from 'antd/es/table';

const DataMaskingPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [tableData, setTableData] = useState<DataMaskingVO[]>([]);

  /**
   * 查詢數據
   */
  const queryData = async () => {
    setLoading(true);
    try {
      const result = await dataMaskingApi.query();
      setTableData(result.data);
    } catch (error) {
      console.error('查詢脫敏數據失敗:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 初始化加載
   */
  useEffect(() => {
    queryData();
  }, []);

  /**
   * 表格列配置
   */
  const columns: ColumnsType<DataMaskingVO> = [
    {
      title: '用戶ID',
      dataIndex: 'userId',
      key: 'userId',
      width: 70,
    },
    {
      title: '默認',
      dataIndex: 'other',
      key: 'other',
      width: 100,
    },
    {
      title: '手機號',
      dataIndex: 'phone',
      key: 'phone',
      width: 100,
    },
    {
      title: '身份證',
      dataIndex: 'idCard',
      key: 'idCard',
      width: 150,
    },
    {
      title: '密碼',
      dataIndex: 'password',
      key: 'password',
      width: 100,
    },
    {
      title: '郵箱',
      dataIndex: 'email',
      key: 'email',
      width: 120,
    },
    {
      title: '車牌號',
      dataIndex: 'carLicense',
      key: 'carLicense',
      width: 120,
    },
    {
      title: '銀行卡',
      dataIndex: 'bankCard',
      key: 'bankCard',
      width: 170,
    },
    {
      title: '地址',
      dataIndex: 'address',
      key: 'address',
      width: 210,
    },
  ];

  return (
    <Card size="small" bordered={false} hoverable>
      {/* 說明 Alert */}
      <Alert
        message={<h4>數據脫敏 Data Masking 介紹：</h4>}
        description={
          <pre style={{ whiteSpace: 'pre-wrap' }}>
            {`簡介：《信息安全技術 網絡安全等級保護基本要求》明確規定，二級以上保護則需要對敏感數據進行脫敏處理。
原理：數據脫敏是指對某些敏感信息通過脫敏規則進行數據的變形，實現敏感隱私數據的可靠保護。
舉例：在不違反系統規則條件下，身份證號、手機號、卡號、客戶號等個人信息都需要進行數據脫敏。

使用方式：
1）脫敏注解 @DataMasking ，支持數據類型如：用戶ID、手機號、密碼、地址、銀行卡、車牌號等；
2）脫敏工具類： SmartDataMaskingUtil ；`}
          </pre>
        }
      />

      {/* 查詢按鈕 */}
      <Form layout="inline" style={{ marginTop: 16, marginBottom: 16 }}>
        <Form.Item>
          <Button type="primary" icon={<SearchOutlined />} onClick={queryData} loading={loading}>
            查詢
          </Button>
        </Form.Item>
      </Form>

      {/* 表格 */}
      <Table
        size="small"
        bordered
        scroll={{ x: 1100 }}
        loading={loading}
        columns={columns}
        dataSource={tableData}
        rowKey="userId"
        pagination={false}
      />
    </Card>
  );
};

export default DataMaskingPage;
