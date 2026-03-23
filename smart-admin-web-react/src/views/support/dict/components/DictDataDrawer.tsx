/**
 * Dict Data Drawer
 * 字典值 Drawer
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import React, { useEffect, useState, useMemo } from 'react';
import { Drawer, Form, Input, Button, Table, Switch, Space, message, Modal } from 'antd';
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
import { DICT_DATA_PERMISSION, DICT_DATA_TABLE_COLUMNS_WIDTH } from '@/constants/support/dictConst';
import BooleanSelect from '@/components/BooleanSelect';
import type { DictDataVO } from '../types';
import DictDataFormModal from './DictDataFormModal';

const { confirm } = Modal;

export interface DictDataDrawerProps {
  visible: boolean;
  dictId?: number;
  dictCode?: string;
  onClose: () => void;
}

const DictDataDrawer: React.FC<DictDataDrawerProps> = ({ visible, dictId, dictCode, onClose }) => {
  const [form] = Form.useForm();
  const hasAddPrivilege = usePrivilege(DICT_DATA_PERMISSION.ADD);
  const hasUpdatePrivilege = usePrivilege(DICT_DATA_PERMISSION.UPDATE);
  const hasDeletePrivilege = usePrivilege(DICT_DATA_PERMISSION.DELETE);

  // 表格數據
  const [dictDataList, setDictDataList] = useState<DictDataVO[]>([]);
  const [tableLoading, setTableLoading] = useState(false);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  // Modal狀態
  const [dictDataFormModalVisible, setDictDataFormModalVisible] = useState(false);
  const [currentDictData, setCurrentDictData] = useState<DictDataVO | undefined>();

  // 查詢參數
  const [keywords, setKeywords] = useState<string | undefined>();
  const [disabledFlag, setDisabledFlag] = useState<number | null>(null);

  // 查詢數據
  const fetchData = async () => {
    if (!dictId) return;

    try {
      setTableLoading(true);
      const res = await dictApi.queryDictData(dictId);

      // 轉換為 DictDataVO 格式
      const dataWithEnabled = res.data.map(item => ({
        dictDataId: 0, // TODO: 需要從 API 返回
        dictId: dictId,
        dictCode: item.dictCode,
        dataValue: item.dataValue,
        dataLabel: item.dataLabel,
        sortOrder: item.dataSort,
        remark: item.remark,
        disabledFlag: item.dictDisabledFlag ? 1 : 0,
        enabled: !item.dictDisabledFlag,
      }));

      setDictDataList(dataWithEnabled as any);
    } catch (error) {
      console.error('Failed to fetch dict data:', error);
    } finally {
      setTableLoading(false);
    }
  };

  // 當 visible 或 dictId 變化時，重新查詢
  useEffect(() => {
    if (visible && dictId) {
      fetchData();
      resetFilters();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible, dictId]);

  // 前端過濾數據
  const filteredTableData = useMemo(() => {
    return dictDataList.filter(item => {
      // 關鍵字過濾
      let keywordsMatch = true;
      if (keywords) {
        const lowerKeywords = keywords.toLowerCase();
        keywordsMatch =
          item.dataValue.toLowerCase().includes(lowerKeywords) ||
          item.dataLabel.toLowerCase().includes(lowerKeywords) ||
          (item.remark?.toLowerCase().includes(lowerKeywords) ?? false);
      }

      // 禁用狀態過濾
      const disabledMatch = disabledFlag === null ? true : item.disabledFlag === disabledFlag;

      return keywordsMatch && disabledMatch;
    });
  }, [dictDataList, keywords, disabledFlag]);

  // 處理搜索
  const handleSearch = () => {
    // 過濾由 useMemo 自動處理
  };

  // 重置過濾器
  const resetFilters = () => {
    setKeywords(undefined);
    setDisabledFlag(null);
    form.resetFields();
  };

  // 處理啟用/禁用切換
  const handleChangeDisabled = async (_checked: boolean, record: DictDataVO) => {
    try {
      await dictApi.updateDictDataDisabled(record.dictDataId);
      message.success('操作成功');
      fetchData();
    } catch (error) {
      console.error('Failed to update disabled status:', error);
    }
  };

  // 打開添加/編輯 Modal
  const handleAddOrUpdate = (record?: DictDataVO) => {
    setCurrentDictData(record);
    setDictDataFormModalVisible(true);
  };

  // 批量刪除
  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('請選擇要刪除的值');
      return;
    }

    confirm({
      title: '提示',
      icon: <ExclamationCircleOutlined />,
      content: '確定要刪除選中值嗎?',
      okText: '刪除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await dictApi.batchDeleteDictData(selectedRowKeys as number[]);
          message.success('刪除成功');
          setSelectedRowKeys([]);
          fetchData();
        } catch (error) {
          console.error('Failed to batch delete dict data:', error);
        }
      },
    });
  };

  // 表格列定義
  const columns: ColumnsType<DictDataVO> = [
    {
      title: '值',
      dataIndex: 'dataValue',
      key: 'dataValue',
      width: DICT_DATA_TABLE_COLUMNS_WIDTH.dataValue,
    },
    {
      title: '名稱',
      dataIndex: 'dataLabel',
      key: 'dataLabel',
      width: DICT_DATA_TABLE_COLUMNS_WIDTH.dataLabel,
    },
    {
      title: '狀態',
      dataIndex: 'disabledFlag',
      key: 'disabledFlag',
      width: DICT_DATA_TABLE_COLUMNS_WIDTH.disabledFlag,
      render: (_: number, record: DictDataVO) => (
        <Switch
          checked={record.enabled}
          checkedChildren="啟用中"
          unCheckedChildren="已禁用"
          onChange={_checked => handleChangeDisabled(_checked, record)}
        />
      ),
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: DICT_DATA_TABLE_COLUMNS_WIDTH.sortOrder,
    },
    {
      title: '備註',
      dataIndex: 'remark',
      key: 'remark',
      width: DICT_DATA_TABLE_COLUMNS_WIDTH.remark,
      ellipsis: true,
    },
    {
      title: '更新時間',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: DICT_DATA_TABLE_COLUMNS_WIDTH.updateTime,
    },
    {
      title: '操作',
      dataIndex: 'action',
      key: 'action',
      width: DICT_DATA_TABLE_COLUMNS_WIDTH.action,
      render: (_: any, record: DictDataVO) => (
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
    <>
      <Drawer
        open={visible}
        title="字典值"
        width={1000}
        onClose={onClose}
        bodyStyle={{ paddingBottom: 80 }}
      >
        {/* 搜索表單 */}
        <Form form={form} layout="inline" style={{ marginBottom: 16 }}>
          <Form.Item label="關鍵字">
            <Input
              value={keywords}
              onChange={e => setKeywords(e.target.value)}
              placeholder="關鍵字"
              allowClear
              style={{ width: 300 }}
            />
          </Form.Item>

          <Form.Item label="禁用">
            <BooleanSelect value={disabledFlag} onChange={setDisabledFlag} style={{ width: 150 }} />
          </Form.Item>

          <Form.Item>
            <Space>
              <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                查詢
              </Button>
              <Button icon={<ReloadOutlined />} onClick={resetFilters}>
                重置
              </Button>
            </Space>
          </Form.Item>
        </Form>

        {/* 操作按鈕 */}
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

        {/* 數據表格 */}
        <Table
          rowKey="dictDataId"
          columns={columns}
          dataSource={filteredTableData}
          loading={tableLoading}
          pagination={false}
          rowSelection={{
            selectedRowKeys,
            onChange: setSelectedRowKeys,
          }}
          size="small"
          bordered
        />

        <div style={{ marginTop: 16, textAlign: 'right' }}>共計 {filteredTableData.length} 條</div>
      </Drawer>

      {/* 字典值表單 Modal */}
      <DictDataFormModal
        visible={dictDataFormModalVisible}
        dictData={currentDictData}
        dictId={dictId!}
        dictCode={dictCode!}
        onClose={() => {
          setDictDataFormModalVisible(false);
          setCurrentDictData(undefined);
        }}
        onSuccess={() => {
          setDictDataFormModalVisible(false);
          setCurrentDictData(undefined);
          fetchData();
        }}
      />
    </>
  );
};

export default DictDataDrawer;
