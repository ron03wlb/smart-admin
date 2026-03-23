/**
 * Department Management Page
 * 部門管理頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/department/
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import { Card, Input, Button, Table, Space, Modal, message, Typography } from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import type { TableColumnsType } from 'antd';
import { usePrivilege } from '@/hooks/usePrivilege';
import PrivilegeButton from '@/components/PrivilegeButton';
import { departmentApi } from '@/api/system/departmentApi';
import type { DepartmentVO, DepartmentFormData } from './types';
import {
  DEPARTMENT_PERMISSION,
  DEPARTMENT_TABLE_COLUMNS_WIDTH,
  DEPARTMENT_CONSTANTS,
} from '@/constants/system/departmentConst';
import { formatDateTime } from '@/utils/date';
import DepartmentFormModal from './components/DepartmentFormModal';

const { Title } = Typography;
const { Search } = Input;

/**
 * 部門管理頁面
 */
export default function DepartmentPage() {
  const hasQueryPrivilege = usePrivilege(DEPARTMENT_PERMISSION.QUERY);
  const hasUpdatePrivilege = usePrivilege(DEPARTMENT_PERMISSION.UPDATE);
  const hasDeletePrivilege = usePrivilege(DEPARTMENT_PERMISSION.DELETE);

  // ==================== State Management ====================

  const [loading, setLoading] = useState(false);
  const [keyword, setKeyword] = useState('');
  const [departmentList, setDepartmentList] = useState<DepartmentVO[]>([]);
  const [departmentTreeData, setDepartmentTreeData] = useState<DepartmentVO[]>([]);
  const [topDepartmentId, setTopDepartmentId] = useState<number | undefined>();
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);

  // Modal state
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formInitialData, setFormInitialData] = useState<DepartmentFormData | undefined>();

  // ==================== Data Loading ====================

  /**
   * 查詢部門列表並構建樹形結構
   */
  const queryDepartmentTree = useCallback(async () => {
    try {
      setLoading(true);
      const response = await departmentApi.queryAllDepartment();
      const data = response.data || [];

      setDepartmentList(data);

      // 構建樹形數據
      const treeData = buildDepartmentTree(data, DEPARTMENT_CONSTANTS.TOP_PARENT_ID);
      setDepartmentTreeData(treeData);

      // 設置頂級部門ID並默認展開
      if (treeData.length > 0) {
        const topId = treeData[0].departmentId;
        setTopDepartmentId(topId);
        setExpandedRowKeys([topId]);
      }
    } catch (error) {
      message.error('查詢部門列表失敗');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    queryDepartmentTree();
  }, [queryDepartmentTree]);

  // ==================== Tree Building ====================

  /**
   * 構建部門樹
   */
  const buildDepartmentTree = (data: DepartmentVO[], parentId: number): DepartmentVO[] => {
    const children = data.filter(item => item.parentId === parentId);

    if (children.length === 0) {
      return [];
    }

    return children.map(item => {
      const node = { ...item };
      const subChildren = buildDepartmentTree(data, item.departmentId);
      if (subChildren.length > 0) {
        node.children = subChildren;
      }
      return node;
    });
  };

  /**
   * 創建部門ID映射表（用於搜索）
   */
  const departmentMap = useMemo(() => {
    const map = new Map<number, DepartmentVO>();
    departmentList.forEach(dept => {
      map.set(dept.departmentId, dept);
    });
    return map;
  }, [departmentList]);

  // ==================== Search Operations ====================

  /**
   * 處理搜索
   */
  const handleSearch = () => {
    if (!keyword.trim()) {
      // 重置為完整樹
      const treeData = buildDepartmentTree(departmentList, DEPARTMENT_CONSTANTS.TOP_PARENT_ID);
      setDepartmentTreeData(treeData);
      return;
    }

    // 篩選包含關鍵字的部門
    const matchedDepartments = departmentList.filter(dept =>
      dept.departmentName.includes(keyword.trim())
    );

    // 遞歸找出所有相關的父級部門
    const relatedDepartments: DepartmentVO[] = [];
    matchedDepartments.forEach(dept => {
      recursiveFindParents(dept.departmentId, relatedDepartments);
    });

    // 構建樹形數據
    const filteredTree = buildDepartmentTree(
      relatedDepartments,
      DEPARTMENT_CONSTANTS.TOP_PARENT_ID
    );
    setDepartmentTreeData(filteredTree);
  };

  /**
   * 遞歸查找父級部門
   */
  const recursiveFindParents = (departmentId: number, resultList: DepartmentVO[]) => {
    const dept = departmentMap.get(departmentId);
    if (!dept || resultList.some(item => item.departmentId === departmentId)) {
      return;
    }

    resultList.push(dept);

    if (dept.parentId && dept.parentId !== DEPARTMENT_CONSTANTS.TOP_PARENT_ID) {
      recursiveFindParents(dept.parentId, resultList);
    }
  };

  /**
   * 處理重置
   */
  const handleReset = () => {
    setKeyword('');
    const treeData = buildDepartmentTree(departmentList, DEPARTMENT_CONSTANTS.TOP_PARENT_ID);
    setDepartmentTreeData(treeData);
  };

  // ==================== Table Columns Definition ====================

  const columns: TableColumnsType<DepartmentVO> = [
    {
      title: '部門名稱',
      dataIndex: 'departmentName',
      key: 'departmentName',
      width: DEPARTMENT_TABLE_COLUMNS_WIDTH.departmentName,
    },
    {
      title: '負責人',
      dataIndex: 'managerName',
      key: 'managerName',
      width: DEPARTMENT_TABLE_COLUMNS_WIDTH.managerName,
      render: text => text || '-',
    },
    {
      title: '排序',
      dataIndex: 'sort',
      key: 'sort',
      width: DEPARTMENT_TABLE_COLUMNS_WIDTH.sort,
    },
    {
      title: '創建時間',
      dataIndex: 'createTime',
      key: 'createTime',
      width: DEPARTMENT_TABLE_COLUMNS_WIDTH.createTime,
      render: text => (text ? formatDateTime(text) : '-'),
    },
    {
      title: '更新時間',
      dataIndex: 'updateTime',
      key: 'updateTime',
      width: DEPARTMENT_TABLE_COLUMNS_WIDTH.updateTime,
      render: text => (text ? formatDateTime(text) : '-'),
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: DEPARTMENT_TABLE_COLUMNS_WIDTH.operate,
      render: (_: any, record: DepartmentVO) => (
        <Space size="small">
          <PrivilegeButton
            privilege={DEPARTMENT_PERMISSION.ADD}
            type="link"
            size="small"
            onClick={() => handleAddSubDepartment(record)}
          >
            添加下級
          </PrivilegeButton>

          {hasUpdatePrivilege && (
            <Button type="link" size="small" onClick={() => handleEdit(record)}>
              編輯
            </Button>
          )}

          {hasDeletePrivilege && record.departmentId !== topDepartmentId && (
            <Button
              type="link"
              size="small"
              danger
              onClick={() => handleDelete(record.departmentId)}
            >
              刪除
            </Button>
          )}
        </Space>
      ),
    },
  ];

  // ==================== CRUD Operations ====================

  /**
   * 處理新增部門
   */
  const handleAdd = () => {
    setFormInitialData({
      parentId: DEPARTMENT_CONSTANTS.TOP_PARENT_ID,
      sort: 0,
    });
    setFormModalVisible(true);
  };

  /**
   * 處理新增子部門
   */
  const handleAddSubDepartment = (parent: DepartmentVO) => {
    setFormInitialData({
      parentId: parent.departmentId,
      sort: 0,
    });
    setFormModalVisible(true);
  };

  /**
   * 處理編輯部門
   */
  const handleEdit = (record: DepartmentVO) => {
    setFormInitialData({
      departmentId: record.departmentId,
      departmentName: record.departmentName,
      parentId: record.parentId,
      managerId: record.managerId,
      sort: record.sort,
    });
    setFormModalVisible(true);
  };

  /**
   * 表單提交成功回調
   */
  const handleFormSuccess = () => {
    setFormModalVisible(false);
    queryDepartmentTree(); // 刷新列表
  };

  /**
   * 處理刪除部門
   */
  const handleDelete = (departmentId: number) => {
    Modal.confirm({
      title: '確認刪除',
      icon: <ExclamationCircleOutlined />,
      content: '確定要刪除該部門嗎？',
      okText: '確定',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          await departmentApi.deleteDepartment(departmentId);
          message.success('刪除成功');
          queryDepartmentTree();
        } catch (error) {
          message.error('刪除失敗');
        }
      },
    });
  };

  // ==================== Render ====================

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        {/* Header */}
        <div style={{ marginBottom: 16 }}>
          <Title level={5} style={{ marginBottom: 16 }}>
            部門管理
          </Title>

          {/* Search Bar */}
          <Space style={{ marginBottom: 16, width: '100%' }} wrap>
            <Search
              placeholder="請輸入部門名稱"
              allowClear
              style={{ width: 300 }}
              value={keyword}
              onChange={e => setKeyword(e.target.value)}
              onSearch={handleSearch}
              enterButton={
                <Button type="primary" icon={<SearchOutlined />} disabled={!hasQueryPrivilege}>
                  查詢
                </Button>
              }
            />

            <Button icon={<ReloadOutlined />} onClick={handleReset} disabled={!hasQueryPrivilege}>
              重置
            </Button>

            <PrivilegeButton
              privilege={DEPARTMENT_PERMISSION.ADD}
              type="primary"
              icon={<PlusOutlined />}
              onClick={handleAdd}
            >
              新建
            </PrivilegeButton>
          </Space>
        </div>

        {/* Table */}
        <Table
          rowKey="departmentId"
          columns={columns}
          dataSource={departmentTreeData}
          loading={loading}
          pagination={false}
          expandable={{
            expandedRowKeys,
            onExpandedRowsChange: keys => setExpandedRowKeys([...keys]),
          }}
          bordered
          size="small"
        />
      </Card>

      {/* 部門表單 Modal */}
      <DepartmentFormModal
        visible={formModalVisible}
        onCancel={() => setFormModalVisible(false)}
        onSuccess={handleFormSuccess}
        initialData={formInitialData}
      />
    </div>
  );
}
