/**
 * CodeGenerator List Page
 *
 * Main page with table name search, paginated table, and action buttons
 * for config, preview, and download.
 */
import { useState, useEffect, useCallback, useRef } from 'react';
import { Table, Button, Input, Space, Card, Form, Pagination, message } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { codeGeneratorApi } from '@/api/support/code-generator-api';
import type { TableInfo, CodeGeneratorQueryForm } from '@/types/code-generator.types';
import { CodeGeneratorConfigDrawer } from './components/form/CodeGeneratorConfigDrawer';
import type { CodeGeneratorConfigDrawerRef } from './components/form/CodeGeneratorConfigDrawer';

const PAGE_SIZE_OPTIONS = ['10', '15', '20', '30', '50', '100'];

const columns: ColumnsType<TableInfo> = [
  {
    title: '序号',
    dataIndex: 'seq',
    width: 50,
    render: (_v, _r, index) => index + 1,
  },
  {
    title: '表名',
    dataIndex: 'tableName',
  },
  {
    title: '备注',
    dataIndex: 'tableComment',
    ellipsis: true,
  },
  {
    title: '代码配置时间',
    dataIndex: 'configTime',
    width: 150,
  },
];

export default function CodeGeneratorList() {
  const [queryForm, setQueryForm] = useState<CodeGeneratorQueryForm>({
    pageNum: 1,
    pageSize: 10,
    tableNameKeywords: undefined,
  });
  const [tableData, setTableData] = useState<TableInfo[]>([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const configDrawerRef = useRef<CodeGeneratorConfigDrawerRef>(null);

  const fetchData = useCallback(async (form: CodeGeneratorQueryForm) => {
    setLoading(true);
    try {
      const res = await codeGeneratorApi.queryTableList(form);
      if (res.success) {
        setTableData(res.data.list);
        setTotal(res.data.total);
      }
    } catch {
      message.error('查询表列表失败');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchData(queryForm);
  }, [queryForm, fetchData]);

  const onSearch = () => {
    setQueryForm((prev) => ({ ...prev, pageNum: 1 }));
  };

  const onReset = () => {
    setQueryForm({ pageNum: 1, pageSize: 10, tableNameKeywords: undefined });
  };

  const showConfig = (record: TableInfo) => {
    configDrawerRef.current?.open(record);
  };

  const showPreview = (record: TableInfo) => {
    // Preview will be implemented in Sprint 5.5
    message.info(`预览功能开发中: ${record.tableName}`);
  };

  const download = (record: TableInfo) => {
    codeGeneratorApi.downloadCode(record.tableName);
  };

  const actionColumn: ColumnsType<TableInfo>[number] = {
    title: '操作',
    dataIndex: 'action',
    fixed: 'right',
    width: 210,
    render: (_v, record) => (
      <div className="smart-table-operate">
        <Button type="link" onClick={() => showConfig(record)}>代码配置</Button>
        <Button type="link" onClick={() => showPreview(record)}>代码预览</Button>
        <Button type="link" onClick={() => download(record)}>下载代码</Button>
      </div>
    ),
  };

  return (
    <div>
      <Form className="smart-query-form" layout="inline" style={{ marginBottom: 16 }}>
        <Form.Item label="表名">
          <Input
            style={{ width: 300 }}
            value={queryForm.tableNameKeywords}
            onChange={(e) => setQueryForm((prev) => ({ ...prev, tableNameKeywords: e.target.value || undefined }))}
            placeholder="请输入表名关键字"
            onPressEnter={onSearch}
          />
        </Form.Item>
        <Form.Item>
          <Space>
            <Button type="primary" icon={<SearchOutlined />} onClick={onSearch}>查询</Button>
            <Button icon={<ReloadOutlined />} onClick={onReset}>重置</Button>
          </Space>
        </Form.Item>
      </Form>

      <Card size="small" bordered={false} hoverable>
        <Table<TableInfo>
          size="small"
          scroll={{ x: 1000 }}
          loading={loading}
          bordered
          dataSource={tableData}
          columns={[...columns, actionColumn]}
          rowKey="tableName"
          pagination={false}
        />

        <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: 16 }}>
          <Pagination
            showSizeChanger
            showQuickJumper
            pageSizeOptions={PAGE_SIZE_OPTIONS}
            current={queryForm.pageNum}
            pageSize={queryForm.pageSize}
            total={total}
            showTotal={(t) => `共${t}条`}
            onChange={(page, size) => setQueryForm((prev) => ({ ...prev, pageNum: page, pageSize: size }))}
          />
        </div>
      </Card>

      <CodeGeneratorConfigDrawer ref={configDrawerRef} onReloadList={() => fetchData(queryForm)} />
    </div>
  );
}
