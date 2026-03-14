/**
 * Message Receiver Modal
 * 消息接收人選擇模態框
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/message/components/message-receiver-modal.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { useState, useImperativeHandle, forwardRef } from 'react';
import { Modal, Form, Input, Button, Table, Pagination, Row, Space } from 'antd';
import { SearchOutlined, ReloadOutlined } from '@ant-design/icons';
import { employeeApi } from '@/api/system/employeeApi';
import type { EmployeeVO } from '@/api/system/employeeApi';

/**
 * 員工查詢表單
 */
interface EmployeeQueryForm {
  searchWord?: string;
  keyword?: string;
  pageNum: number;
  pageSize: number;
}

/**
 * Props
 */
interface MessageReceiverModalProps {
  onConfirm: (employeeIds: number[], employeeNames: string[]) => void;
}

/**
 * Ref Methods
 */
export interface MessageReceiverModalRef {
  showModal: (selectedIds?: number[]) => void;
}

const MessageReceiverModal = forwardRef<MessageReceiverModalRef, MessageReceiverModalProps>(
  ({ onConfirm }, ref) => {
    const [visible, setVisible] = useState(false);
    const [loading, setLoading] = useState(false);
    const [tableData, setTableData] = useState<EmployeeVO[]>([]);
    const [total, setTotal] = useState(0);
    const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);
    const [selectedRows, setSelectedRows] = useState<EmployeeVO[]>([]);

    const [queryForm, setQueryForm] = useState<EmployeeQueryForm>({
      searchWord: undefined,
      keyword: undefined,
      pageNum: 1,
      pageSize: 10,
    });

    /**
     * 查詢員工列表
     */
    const queryList = async () => {
      setLoading(true);
      try {
        const result = await employeeApi.queryEmployee(queryForm);
        setTableData(result.data.list);
        setTotal(result.data.total);
      } catch (error) {
        console.error('查詢員工列表失敗:', error);
      } finally {
        setLoading(false);
      }
    };

    /**
     * 搜索
     */
    const handleSearch = () => {
      setQueryForm((prev) => ({
        ...prev,
        keyword: prev.searchWord,
        pageNum: 1,
      }));
      setTimeout(() => queryList(), 0);
    };

    /**
     * 重置
     */
    const handleReset = () => {
      const pageSize = queryForm.pageSize;
      setQueryForm({
        searchWord: undefined,
        keyword: undefined,
        pageNum: 1,
        pageSize,
      });
      setTimeout(() => queryList(), 0);
    };

    /**
     * 顯示模態框
     */
    const showModal = (selectedIds?: number[]) => {
      setSelectedRowKeys(selectedIds || []);
      setSelectedRows([]);
      setVisible(true);
      setTimeout(() => queryList(), 0);
    };

    /**
     * 關閉模態框
     */
    const handleCancel = () => {
      setVisible(false);
    };

    /**
     * 確認選擇
     */
    const handleOk = () => {
      const employeeIds = selectedRowKeys;
      const employeeNames = selectedRows.map((row) => row.actualName);
      onConfirm(employeeIds, employeeNames);
      setVisible(false);
    };

    /**
     * 分頁變更
     */
    const handlePageChange = (page: number, pageSize: number) => {
      setQueryForm((prev) => ({ ...prev, pageNum: page, pageSize }));
      setTimeout(() => queryList(), 0);
    };

    /**
     * 表格列配置
     */
    const columns = [
      {
        title: '姓名',
        dataIndex: 'actualName',
        key: 'actualName',
        align: 'center' as const,
      },
      {
        title: '手機號',
        dataIndex: 'phone',
        key: 'phone',
        align: 'center' as const,
      },
    ];

    /**
     * 行選擇配置
     */
    const rowSelection = {
      selectedRowKeys,
      onChange: (keys: React.Key[], rows: EmployeeVO[]) => {
        setSelectedRowKeys(keys as number[]);
        setSelectedRows(rows);
      },
    };

    /**
     * 暴露方法給父組件
     */
    useImperativeHandle(ref, () => ({
      showModal,
    }));

    return (
      <Modal
        title="推送人"
        open={visible}
        onOk={handleOk}
        onCancel={handleCancel}
        width={1100}
        okText="確定"
        cancelText="取消"
        style={{ zIndex: 9999 }}
      >
        {/* 查詢表單 */}
        <Form layout="inline" style={{ marginBottom: 16 }}>
          <Form.Item label="關鍵詞搜索">
            <Input
              style={{ width: 250 }}
              placeholder="請輸入姓名"
              value={queryForm.searchWord}
              onChange={(e) => setQueryForm((prev) => ({ ...prev, searchWord: e.target.value }))}
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
        <Table
          rowKey="employeeId"
          loading={loading}
          columns={columns}
          dataSource={tableData}
          bordered
          pagination={false}
          rowSelection={rowSelection}
        />

        {/* 分頁 */}
        <Row justify="end" style={{ marginTop: 16 }}>
          <Pagination
            showSizeChanger
            showQuickJumper
            current={queryForm.pageNum}
            pageSize={queryForm.pageSize}
            total={total}
            onChange={handlePageChange}
            showTotal={(total) => `共 ${total} 條`}
            pageSizeOptions={['10', '20', '30', '50']}
          />
        </Row>
      </Modal>
    );
  }
);

MessageReceiverModal.displayName = 'MessageReceiverModal';

export default MessageReceiverModal;
