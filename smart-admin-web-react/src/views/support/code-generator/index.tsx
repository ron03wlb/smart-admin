/**
 * Code Generator - 代碼生成器
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/code-generator/code-generator-list.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { useState, useEffect, useRef } from 'react';
import { Form, Input, Button, Card, Table, Space, message } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { codeGeneratorApi, type TableInfo, type TableQueryForm } from '@/api/support/codeGeneratorApi';
import type { PageResult } from '@/api/types/response';
import ConfigDrawer from './components/ConfigDrawer';
import PreviewModal from './components/PreviewModal';

const CodeGeneratorPage: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [dataSource, setDataSource] = useState<TableInfo[]>([]);
  const [total, setTotal] = useState(0);
  const [queryForm, setQueryForm] = useState<TableQueryForm>({
    tableNameKeywords: undefined,
    pageNum: 1,
    pageSize: 10,
    searchCount: false,
  });

  const configDrawerRef = useRef<any>(null);
  const previewModalRef = useRef<any>(null);

  /**
   * 列定義
   */
  const columns: ColumnsType<TableInfo> = [
    {
      title: '序號',
      width: 60,
      align: 'center',
      render: (_text, _record, index) => index + 1,
    },
    {
      title: '表名',
      dataIndex: 'tableName',
      width: 200,
    },
    {
      title: '備註',
      dataIndex: 'tableComment',
      ellipsis: true,
    },
    {
      title: '代碼配置時間',
      dataIndex: 'configTime',
      width: 180,
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 230,
      render: (_text, record) => (
        <Space>
          <Button type="link" size="small" onClick={() => showConfig(record)}>
            代碼配置
          </Button>
          <Button type="link" size="small" onClick={() => showPreview(record)}>
            代碼預覽
          </Button>
          <Button type="link" size="small" onClick={() => handleDownload(record)}>
            下載代碼
          </Button>
        </Space>
      ),
    },
  ];

  /**
   * 查詢表列表
   */
  const queryTableList = async () => {
    try {
      setLoading(true);
      const result = await codeGeneratorApi.queryTableList(queryForm);
      const pageResult: PageResult<TableInfo> = result.data;
      setDataSource(pageResult.list || []);
      setTotal(pageResult.total || 0);
    } catch (error) {
      message.error('查詢失敗');
      console.error('Query table list error:', error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * 搜索
   */
  const handleSearch = () => {
    const values = form.getFieldsValue();
    setQueryForm({
      ...queryForm,
      tableNameKeywords: values.tableNameKeywords,
      pageNum: 1,
    });
  };

  /**
   * 重置
   */
  const handleReset = () => {
    form.resetFields();
    setQueryForm({
      tableNameKeywords: undefined,
      pageNum: 1,
      pageSize: 10,
      searchCount: false,
    });
  };

  /**
   * 分頁變化
   */
  const handlePageChange = (page: number, pageSize: number) => {
    setQueryForm({
      ...queryForm,
      pageNum: page,
      pageSize,
    });
  };

  /**
   * 顯示配置 Drawer
   */
  const showConfig = (record: TableInfo) => {
    configDrawerRef.current?.showDrawer(record);
  };

  /**
   * 顯示預覽 Modal
   */
  const showPreview = (record: TableInfo) => {
    previewModalRef.current?.showModal(record);
  };

  /**
   * 下載代碼
   */
  const handleDownload = (record: TableInfo) => {
    codeGeneratorApi.downloadCode(record.tableName);
    message.success('下載請求已發送');
  };

  /**
   * 重新加載列表
   */
  const reloadList = () => {
    queryTableList();
  };

  /**
   * 初始化加載
   */
  useEffect(() => {
    queryTableList();
  }, [queryForm]);

  return (
    <div className="code-generator-page">
      {/* 查詢表單 */}
      <Form form={form} layout="inline" style={{ marginBottom: 16 }}>
        <Form.Item label="表名" name="tableNameKeywords">
          <Input
            style={{ width: 300 }}
            placeholder="請輸入表名關鍵字"
            onPressEnter={handleSearch}
          />
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

      {/* 表格 */}
      <Card size="small" bordered={false}>
        <Table
          size="small"
          loading={loading}
          columns={columns}
          dataSource={dataSource}
          rowKey="tableName"
          scroll={{ x: 1000 }}
          pagination={{
            current: queryForm.pageNum,
            pageSize: queryForm.pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 條`,
            onChange: handlePageChange,
          }}
        />
      </Card>

      {/* 配置 Drawer */}
      <ConfigDrawer ref={configDrawerRef} onReload={reloadList} />

      {/* 預覽 Modal */}
      <PreviewModal ref={previewModalRef} />
    </div>
  );
};

export default CodeGeneratorPage;
