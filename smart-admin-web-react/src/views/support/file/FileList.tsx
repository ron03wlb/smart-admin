/**
 * File List
 *
 * Corresponds to Vue's support/file/file-list.vue (296L)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Card, Table, Input, Button, Space, DatePicker } from 'antd';
import { fileApi } from '@/api/support/file-api';
import type { FileVO } from '@/types/file.types';
import type { ColumnsType } from 'antd/es/table';
import type { DateRangeValue } from '@/types/date-range.types';

const { RangePicker } = DatePicker;
const PAGE_SIZE = 10;

function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB', 'TB'];
  const i = Math.floor(Math.log(bytes) / Math.log(1024));
  return `${(bytes / Math.pow(1024, i)).toFixed(2)} ${units[i]}`;
}

const FileList: React.FC = () => {
  const [data, setData] = useState<FileVO[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [pageNum, setPageNum] = useState(1);
  const [fileName, setFileName] = useState('');
  const [fileKey, setFileKey] = useState('');
  const [dateRange, setDateRange] = useState<DateRangeValue>(null);

  const queryList = useCallback(async (page: number) => {
    setLoading(true);
    try {
      const res = await fileApi.queryPage({
        fileName,
        fileKey,
        startDate: dateRange?.[0]?.format('YYYY-MM-DD'),
        endDate: dateRange?.[1]?.format('YYYY-MM-DD'),
        pageNum: page,
        pageSize: PAGE_SIZE,
      });
      if (res.code === 1 && res.data) {
        setData(res.data.list || []);
        setTotal(res.data.total || 0);
      }
    } finally {
      setLoading(false);
    }
  }, [fileName, fileKey, dateRange]);

  useEffect(() => {
    queryList(1);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleSearch = () => {
    setPageNum(1);
    queryList(1);
  };

  const handleReset = () => {
    setFileName('');
    setFileKey('');
    setDateRange(null);
    setPageNum(1);
    queryList(1);
  };

  const columns: ColumnsType<FileVO> = [
    { title: '文件名', dataIndex: 'fileName', ellipsis: true },
    { title: '大小', dataIndex: 'fileSize', width: 100, render: (val) => formatFileSize(val) },
    { title: '类型', dataIndex: 'fileType', width: 80 },
    { title: 'FileKey', dataIndex: 'fileKey', ellipsis: true, width: 200 },
    { title: '上传人', dataIndex: 'creatorName', width: 100 },
    { title: '时间', dataIndex: 'createTime', width: 180 },
    {
      title: '操作',
      width: 100,
      align: 'center',
      render: (_, record) => (
        <a onClick={() => fileApi.downloadFile(record.fileKey)}>下载</a>
      ),
    },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input style={{ width: 160 }} value={fileName} onChange={(e) => setFileName(e.target.value)} placeholder="文件名" allowClear />
        <Input style={{ width: 160 }} value={fileKey} onChange={(e) => setFileKey(e.target.value)} placeholder="FileKey" allowClear />
        <RangePicker value={dateRange} onChange={(val) => setDateRange(val)} />
        <Button type="primary" onClick={handleSearch}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
      </Space>

      <Table
        rowKey="fileId"
        columns={columns}
        dataSource={data}
        loading={loading}
        size="small"
        pagination={{
          current: pageNum,
          pageSize: PAGE_SIZE,
          total,
          showTotal: (t) => `共${t}条`,
          onChange: (page) => { setPageNum(page); queryList(page); },
        }}
      />
    </Card>
  );
};

export default FileList;
