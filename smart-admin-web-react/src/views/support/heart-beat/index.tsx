/**
 * Heart Beat List Page
 * 心跳記錄列表頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import React, { useEffect, useState } from 'react';
import { Card, Alert, Form, Input, Button, Table, DatePicker, Space } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { type Dayjs } from 'dayjs';
import { heartBeatApi } from '@/api/support/heartBeatApi';
import { HEART_BEAT_TABLE_COLUMNS_WIDTH } from '@/constants/support/heartBeatConst';
import type { HeartBeatVO, HeartBeatQueryForm } from './types';

const { RangePicker } = DatePicker;

const HeartBeatListPage: React.FC = () => {
  const [form] = Form.useForm<HeartBeatQueryForm>();
  const [tableData, setTableData] = useState<HeartBeatVO[]>([]);
  const [tableLoading, setTableLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [createDateRange, setCreateDateRange] = useState<[Dayjs, Dayjs] | null>(null);

  useEffect(() => {
    form.setFieldsValue({
      pageNum: 1,
      pageSize: 10,
    });
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setTableLoading(true);
      const values = form.getFieldsValue();
      const res = await heartBeatApi.queryList(values);

      setTableData(res.data.list);
      setTotal(res.data.total);
    } catch (error) {
      console.error('Failed to fetch heart beat list:', error);
    } finally {
      setTableLoading(false);
    }
  };

  const handleSearch = () => {
    form.setFieldsValue({ pageNum: 1 });
    fetchData();
  };

  const handleReset = () => {
    form.resetFields();
    form.setFieldsValue({
      pageNum: 1,
      pageSize: 10,
    });
    setCreateDateRange(null);
    fetchData();
  };

  const handleTableChange = (page: number, pageSize: number) => {
    form.setFieldsValue({ pageNum: page, pageSize });
    fetchData();
  };

  const handleDateChange = (dates: any, _dateStrings: [string, string]) => {
    setCreateDateRange(dates);
    if (dates) {
      form.setFieldsValue({
        startDate: dates[0].format('YYYY-MM-DD'),
        endDate: dates[1].format('YYYY-MM-DD'),
      });
    } else {
      form.setFieldsValue({
        startDate: null,
        endDate: null,
      });
    }
  };

  const columns: ColumnsType<HeartBeatVO> = [
    {
      title: '項目路徑',
      dataIndex: 'projectPath',
      key: 'projectPath',
      width: HEART_BEAT_TABLE_COLUMNS_WIDTH.projectPath,
      ellipsis: true,
    },
    {
      title: '服務器IP',
      dataIndex: 'serverIp',
      key: 'serverIp',
      width: HEART_BEAT_TABLE_COLUMNS_WIDTH.serverIp,
      ellipsis: true,
    },
    {
      title: '進程號',
      dataIndex: 'processNo',
      key: 'processNo',
      width: HEART_BEAT_TABLE_COLUMNS_WIDTH.processNo,
    },
    {
      title: '進程開啟時間',
      dataIndex: 'processStartTime',
      key: 'processStartTime',
      width: HEART_BEAT_TABLE_COLUMNS_WIDTH.processStartTime,
    },
    {
      title: '心跳當前時間',
      dataIndex: 'heartBeatTime',
      key: 'heartBeatTime',
      width: HEART_BEAT_TABLE_COLUMNS_WIDTH.heartBeatTime,
    },
  ];

  return (
    <Card size="small" bordered={false} hoverable>
      <Alert
        message={<h4>Smart-Heart-Beat 心跳服務介紹：</h4>}
        description={
          <pre>
            {`簡介：Smart-Heart-Beat 是心跳服務，用於監測Java應用的狀態等其他信息。
原理：Java後端會在項目啟動的時候開啟一個線程，每隔一段時間將該應用的IP、進程號更新到數據庫t_heart_beat_record表中。

用途：
1）在各個環境（無論開發、測試、生產）能統一看到所有啟動的服務列表；
2）檢測Java應用是否存活；
3）當某些業務只允許有一個服務啟動的時候，用於排查是否別人也啟動的服務；
4） ※強烈推薦※`}
          </pre>
        }
        type="info"
        style={{ marginBottom: 16 }}
      />

      <Form form={form} layout="inline" style={{ marginBottom: 16 }}>
        <Form.Item label="關鍵字">
          <Input
            value={form.getFieldValue('keywords')}
            onChange={e => form.setFieldValue('keywords', e.target.value)}
            placeholder="關鍵字"
            style={{ width: 300 }}
          />
        </Form.Item>

        <Form.Item label="心跳時間">
          <RangePicker value={createDateRange} onChange={handleDateChange} style={{ width: 240 }} />
        </Form.Item>

        <Form.Item name="pageNum" hidden>
          <Input />
        </Form.Item>

        <Form.Item name="pageSize" hidden>
          <Input />
        </Form.Item>

        <Form.Item name="startDate" hidden>
          <Input />
        </Form.Item>

        <Form.Item name="endDate" hidden>
          <Input />
        </Form.Item>

        <Form.Item>
          <Space>
            <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
              查詢
            </Button>
            <Button icon={<ReloadOutlined />} onClick={handleReset}>
              重置
            </Button>
          </Space>
        </Form.Item>
      </Form>

      <Table
        rowKey="heartBeatId"
        columns={columns}
        dataSource={tableData}
        loading={tableLoading}
        pagination={{
          current: form.getFieldValue('pageNum') || 1,
          pageSize: form.getFieldValue('pageSize') || 10,
          total,
          showSizeChanger: true,
          showQuickJumper: true,
          showTotal: total => `共${total}條`,
          onChange: handleTableChange,
        }}
        size="small"
        bordered
      />
    </Card>
  );
};

export default HeartBeatListPage;
