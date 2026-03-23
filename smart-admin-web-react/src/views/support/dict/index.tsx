/**
 * Dict List Page
 * 字典列表頁面
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import React, { useEffect, useState } from 'react';
import { Form, Input, Button, Card, Table, Switch, Space, message, Modal } from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { dictApi } from '@/api/support/dictApi';
import { usePrivilege } from '@/hooks/usePrivilege';
import { DICT_PERMISSION, DICT_TABLE_COLUMNS_WIDTH } from '@/constants/support/dictConst';
import BooleanSelect from '@/components/BooleanSelect';
import type { DictVO, DictQueryForm } from './types';
import DictFormModal from './components/DictFormModal';
import DictDataDrawer from './components/DictDataDrawer';

const { confirm } = Modal;

/**
 * 字典列表組件
 */
const DictList: React.FC = () => {
  const [form] = Form.useForm<DictQueryForm>();
  const hasAddPrivilege = usePrivilege(DICT_PERMISSION.ADD);
  const hasUpdatePrivilege = usePrivilege(DICT_PERMISSION.UPDATE);
  const hasDeletePrivilege = usePrivilege(DICT_PERMISSION.DELETE);

  // 表格數據
  const [tableData, setTableData] = useState<DictVO[]>([]);
  const [tableLoading, setTableLoading] = useState(false);
  const [total, setTotal] = useState(0);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  // Modal refs
  const [dictFormModalVisible, setDictFormModalVisible] = useState(false);
  const [dictDataDrawerVisible, setDictDataDrawerVisible] = useState(false);
  const [currentDict, setCurrentDict] = useState<DictVO | undefined>();
  const [currentDictForData, setCurrentDictForData] = useState<
    { dictId: number; dictCode: string } | undefined
  >();

  // 查詢數據
  const fetchData = async () => {
    try {
      setTableLoading(true);
      const values = form.getFieldsValue();
      const res = await dictApi.queryDict({
        keywords: values.keywords,
        disabledFlag: values.disabledFlag,
        pageNum: values.pageNum || 1,
        pageSize: values.pageSize || 10,
      });

      // 轉換 enabled 字段
      const dataWithEnabled = res.data.list.map((item: any) => ({
        ...item,
        enabled: !item.disabledFlag,
      }));

      setTableData(dataWithEnabled);
      setTotal(res.data.total);
    } catch (error) {
      console.error('Failed to fetch dict list:', error);
    } finally {
      setTableLoading(false);
    }
  };

  // 初始化查詢
  useEffect(() => {
    form.setFieldsValue({ pageNum: 1, pageSize: 10 });
    fetchData();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 處理搜索
  const handleSearch = () => {
    form.setFieldsValue({ pageNum: 1 });
    fetchData();
  };

  // 處理重置
  const handleReset = () => {
    form.resetFields();
    form.setFieldsValue({ pageNum: 1, pageSize: 10 });
    fetchData();
  };

  // 處理分頁變化
  const handleTableChange = (page: number, pageSize: number) => {
    form.setFieldsValue({ pageNum: page, pageSize });
    fetchData();
  };

  // 處理啟用/禁用切換
  const handleChangeDisabled = async (_checked: boolean, record: DictVO) => {
    try {
      await dictApi.updateDisabled(record.dictId);
      message.success('操作成功');
      fetchData();
    } catch (error) {
      console.error('Failed to update disabled status:', error);
    }
  };

  // 打開添加/編輯 Modal
  const handleAddOrUpdate = (record?: DictVO) => {
    setCurrentDict(record);
    setDictFormModalVisible(true);
  };

  // 打開字典值 Drawer
  const handleShowDictData = (record: DictVO) => {
    setCurrentDictForData({ dictId: record.dictId, dictCode: record.dictCode });
    setDictDataDrawerVisible(true);
  };

  // 批量刪除
  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('請選擇要刪除的字典');
      return;
    }

    confirm({
      title: '提示',
      icon: <ExclamationCircleOutlined />,
      content: '確定要刪除選中的字典嗎?',
      okText: '刪除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await dictApi.batchDeleteDict(selectedRowKeys as number[]);
          message.success('刪除成功');
          setSelectedRowKeys([]);
          fetchData();
        } catch (error) {
          console.error('Failed to batch delete dict:', error);
        }
      },
    });
  };

  // 表格列定義
  const columns: ColumnsType<DictVO> = [
    {
      title: 'ID',
      dataIndex: 'dictId',
      key: 'dictId',
      width: DICT_TABLE_COLUMNS_WIDTH.dictId,
    },
    {
      title: '編碼',
      dataIndex: 'dictCode',
      key: 'dictCode',
      width: DICT_TABLE_COLUMNS_WIDTH.dictCode,
      render: (text: string, record: DictVO) => (
        <a onClick={() => handleShowDictData(record)}>{text}</a>
      ),
    },
    {
      title: '名稱',
      dataIndex: 'dictName',
      key: 'dictName',
      width: DICT_TABLE_COLUMNS_WIDTH.dictName,
    },
    {
      title: '備註',
      dataIndex: 'remark',
      key: 'remark',
      width: DICT_TABLE_COLUMNS_WIDTH.remark,
      ellipsis: true,
    },
    {
      title: '狀態',
      dataIndex: 'disabledFlag',
      key: 'disabledFlag',
      width: DICT_TABLE_COLUMNS_WIDTH.disabledFlag,
      render: (_: number, record: DictVO) => (
        <Switch
          checked={record.enabled}
          checkedChildren="啟用中"
          unCheckedChildren="已禁用"
          onChange={_checked => handleChangeDisabled(_checked, record)}
        />
      ),
    },
    {
      title: '更新時間',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: DICT_TABLE_COLUMNS_WIDTH.updateTime,
    },
    {
      title: '操作',
      dataIndex: 'action',
      key: 'action',
      fixed: 'right',
      width: DICT_TABLE_COLUMNS_WIDTH.action,
      render: (_: any, record: DictVO) => (
        <Button
          type="link"
          size="small"
          disabled={!hasUpdatePrivilege}
          onClick={() => handleAddOrUpdate(record)}
        >
          編輯
        </Button>
      ),
    },
  ];

  return (
    <div className="dict-list">
      {/* 搜索表單 */}
      <Form form={form} layout="inline" style={{ marginBottom: 16 }}>
        <Form.Item name="keywords" label="關鍵字">
          <Input placeholder="編碼/名稱/備註" allowClear style={{ width: 300 }} />
        </Form.Item>

        <Form.Item name="disabledFlag" label="禁用">
          <BooleanSelect style={{ width: 150 }} />
        </Form.Item>

        <Form.Item name="pageNum" hidden>
          <Input />
        </Form.Item>

        <Form.Item name="pageSize" hidden>
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

      {/* 數據表格 */}
      <Card size="small">
        <div style={{ marginBottom: 16 }}>
          <Space>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              disabled={!hasAddPrivilege}
              onClick={() => handleAddOrUpdate()}
            >
              新建
            </Button>
            <Button
              type="primary"
              danger
              icon={<DeleteOutlined />}
              disabled={!hasDeletePrivilege || selectedRowKeys.length === 0}
              onClick={handleBatchDelete}
            >
              批量刪除
            </Button>
          </Space>
        </div>

        <Table
          rowKey="dictId"
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
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
          }}
          size="small"
          bordered
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* 字典表單 Modal */}
      <DictFormModal
        visible={dictFormModalVisible}
        dict={currentDict}
        onClose={() => {
          setDictFormModalVisible(false);
          setCurrentDict(undefined);
        }}
        onSuccess={() => {
          setDictFormModalVisible(false);
          setCurrentDict(undefined);
          fetchData();
        }}
      />

      {/* 字典值 Drawer */}
      <DictDataDrawer
        visible={dictDataDrawerVisible}
        dictId={currentDictForData?.dictId}
        dictCode={currentDictForData?.dictCode}
        onClose={() => {
          setDictDataDrawerVisible(false);
          setCurrentDictForData(undefined);
        }}
      />
    </div>
  );
};

export default DictList;
