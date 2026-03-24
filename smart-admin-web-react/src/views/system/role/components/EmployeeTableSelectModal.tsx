/**
 * Employee Table Select Modal Component
 * 員工選擇表格 Modal 組件
 *
 * 用於從員工列表中選擇一個或多個員工，支持：
 * - 分頁查詢
 * - 關鍵字搜索（姓名、電話、登錄名）
 * - 批量選擇
 * - 部門過濾
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-24
 */

import { useEffect, useState } from 'react';
import { Modal, Table, Input, Space, Tag, message } from 'antd';
import type { TableColumnsType, TablePaginationConfig } from 'antd';
import { employeeApi } from '@/api/system/employeeApi';
import type { EmployeeVO } from '@/views/system/employee/types';
import type { ResponseDTO } from '@/api/types/response';

const { Search } = Input;

interface EmployeeTableSelectModalProps {
  /** Modal 顯示/隱藏 */
  visible: boolean;

  /** 取消回調 */
  onCancel: () => void;

  /** 確認回調（返回選中的員工ID列表） */
  onConfirm: (selectedEmployeeIds: number[]) => void;

  /** 已選中的員工ID（用於排除已添加的員工） */
  excludeEmployeeIds?: number[];

  /** 部門ID過濾（可選） */
  departmentId?: number;
}

/**
 * 員工選擇表格 Modal
 */
export default function EmployeeTableSelectModal({
  visible,
  onCancel,
  onConfirm,
  excludeEmployeeIds = [],
  departmentId,
}: EmployeeTableSelectModalProps) {
  // ==================== State ====================

  const [loading, setLoading] = useState(false);
  const [employeeList, setEmployeeList] = useState<EmployeeVO[]>([]);
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([]);
  const [keyword, setKeyword] = useState<string>('');
  const [pagination, setPagination] = useState<TablePaginationConfig>({
    current: 1,
    pageSize: 10,
    total: 0,
  });

  // ==================== Effects ====================

  useEffect(() => {
    if (visible) {
      loadEmployeeList();
    } else {
      // Modal 關閉時清空狀態
      setKeyword('');
      setSelectedRowKeys([]);
      setPagination({ current: 1, pageSize: 10, total: 0 });
    }
  }, [visible]);

  // ==================== Data Loading ====================

  /**
   * 加載員工列表
   */
  const loadEmployeeList = async (searchKeyword?: string, page = 1, pageSize = 10) => {
    try {
      setLoading(true);

      const params = {
        keyword: searchKeyword || keyword,
        departmentId,
        pageNum: page,
        pageSize,
      };

      const response: ResponseDTO<{
        list: EmployeeVO[];
        total: number;
        pageNum: number;
        pageSize: number;
      }> = await employeeApi.queryEmployee(params);

      if (response.ok && response.data) {
        // 過濾掉已添加的員工
        const filteredList = response.data.list.filter(
          (employee) => !excludeEmployeeIds.includes(employee.employeeId)
        );

        setEmployeeList(filteredList);
        setPagination({
          current: response.data.pageNum,
          pageSize: response.data.pageSize,
          total: response.data.total,
        });
      }
    } catch (error) {
      console.error('加載員工列表失敗:', error);
      message.error('加載員工列表失敗');
    } finally {
      setLoading(false);
    }
  };

  // ==================== Event Handlers ====================

  /**
   * 處理搜索
   */
  const handleSearch = (value: string) => {
    setKeyword(value);
    loadEmployeeList(value, 1, pagination.pageSize || 10);
  };

  /**
   * 處理分頁變更
   */
  const handleTableChange = (newPagination: TablePaginationConfig) => {
    loadEmployeeList(keyword, newPagination.current || 1, newPagination.pageSize || 10);
  };

  /**
   * 處理行選擇變更
   */
  const handleSelectChange = (newSelectedRowKeys: React.Key[]) => {
    setSelectedRowKeys(newSelectedRowKeys as number[]);
  };

  /**
   * 處理確認
   */
  const handleConfirm = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('請至少選擇一名員工');
      return;
    }

    onConfirm(selectedRowKeys);
    setSelectedRowKeys([]);
  };

  /**
   * 處理取消
   */
  const handleCancel = () => {
    setSelectedRowKeys([]);
    onCancel();
  };

  // ==================== Table Configuration ====================

  const columns: TableColumnsType<EmployeeVO> = [
    {
      title: '姓名',
      dataIndex: 'actualName',
      key: 'actualName',
      width: 120,
    },
    {
      title: '登錄名',
      dataIndex: 'loginName',
      key: 'loginName',
      width: 120,
    },
    {
      title: '電話',
      dataIndex: 'phone',
      key: 'phone',
      width: 130,
    },
    {
      title: '部門',
      dataIndex: 'departmentName',
      key: 'departmentName',
      width: 150,
      ellipsis: true,
    },
    {
      title: '職位',
      dataIndex: 'positionName',
      key: 'positionName',
      width: 120,
      ellipsis: true,
    },
    {
      title: '狀態',
      dataIndex: 'disabledFlag',
      key: 'disabledFlag',
      width: 80,
      render: (disabled: boolean) => (
        <Tag color={disabled ? 'error' : 'success'}>{disabled ? '禁用' : '正常'}</Tag>
      ),
    },
  ];

  const rowSelection = {
    selectedRowKeys,
    onChange: handleSelectChange,
    getCheckboxProps: (record: EmployeeVO) => ({
      disabled: record.disabledFlag, // 禁用的員工不可選擇
    }),
  };

  // ==================== Render ====================

  return (
    <Modal
      title="選擇員工"
      open={visible}
      onCancel={handleCancel}
      onOk={handleConfirm}
      width={900}
      destroyOnClose
      okText="確認添加"
      cancelText="取消"
    >
      <Space direction="vertical" style={{ width: '100%' }} size="middle">
        {/* 搜索框 */}
        <Search
          placeholder="請輸入姓名、登錄名或電話搜索"
          allowClear
          enterButton="搜索"
          onSearch={handleSearch}
          style={{ width: 300 }}
        />

        {/* 選擇提示 */}
        {selectedRowKeys.length > 0 && (
          <div style={{ color: '#1890ff' }}>已選擇 {selectedRowKeys.length} 名員工</div>
        )}

        {/* 員工列表表格 */}
        <Table<EmployeeVO>
          rowKey="employeeId"
          columns={columns}
          dataSource={employeeList}
          pagination={pagination}
          loading={loading}
          rowSelection={rowSelection}
          onChange={handleTableChange}
          scroll={{ y: 400 }}
          size="small"
        />
      </Space>
    </Modal>
  );
}
