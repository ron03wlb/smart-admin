/**
 * Department List Page (Tree Table)
 *
 * Corresponds to Vue's views/system/department/department-list.vue
 */
import React, { useState, useEffect, useCallback, useMemo } from 'react';
import { Table, Input, Space, Modal, Card, Form, message } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons';
import { PrivilegeButton } from '@/components/framework/privilege/PrivilegeButton';
import { departmentApi } from '@/api/system/department-api';
import type { DepartmentVO } from '@/types/department.types';
import DepartmentFormModal from './DepartmentFormModal';

const DEPARTMENT_PARENT_ID = 0;

/** Build tree from flat department list */
function buildDepartmentTree(data: DepartmentVO[], parentId: number): DepartmentVO[] | undefined {
  const children = data.filter((e) => e.parentId === parentId);
  if (children.length === 0) return undefined;
  return children.map((child) => ({
    ...child,
    children: buildDepartmentTree(data, child.departmentId) ?? undefined,
  }));
}

const DepartmentList: React.FC = () => {
  const [keywords, setKeywords] = useState('');
  const [loading, setLoading] = useState(false);
  const [departmentList, setDepartmentList] = useState<DepartmentVO[]>([]);
  const [treeData, setTreeData] = useState<DepartmentVO[]>([]);
  const [defaultExpandedRowKeys, setDefaultExpandedRowKeys] = useState<number[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<Partial<DepartmentVO> | null>(null);

  // Top department ID (first root node)
  const topDepartmentId = useMemo(() => {
    if (treeData.length > 0) return treeData[0].departmentId;
    return undefined;
  }, [treeData]);

  // Build ID map for search filtering
  const idInfoMap = useMemo(() => {
    const map = new Map<number, DepartmentVO>();
    departmentList.forEach((e) => map.set(e.departmentId, e));
    return map;
  }, [departmentList]);

  const queryDepartmentTree = useCallback(async () => {
    setLoading(true);
    try {
      const res = await departmentApi.listAll();
      if (res.code === 1 && res.data) {
        setDepartmentList(res.data);
        const tree = buildDepartmentTree(res.data, DEPARTMENT_PARENT_ID) ?? [];
        setTreeData(tree);
        if (tree.length > 0) {
          setDefaultExpandedRowKeys([tree[0].departmentId]);
        }
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    queryDepartmentTree();
  }, [queryDepartmentTree]);

  // Search: filter by keyword, include parent chain
  const handleSearch = () => {
    if (!keywords) {
      setTreeData(buildDepartmentTree(departmentList, DEPARTMENT_PARENT_ID) ?? []);
      return;
    }
    const filtered = departmentList.filter(
      (e) => e.departmentName.indexOf(keywords) > -1
    );
    const resultList: DepartmentVO[] = [];
    const addedIds = new Set<number>();

    function addWithParents(id: number) {
      if (addedIds.has(id)) return;
      const info = idInfoMap.get(id);
      if (!info) return;
      addedIds.add(id);
      resultList.push(info);
      if (info.parentId && info.parentId !== 0) {
        addWithParents(info.parentId);
      }
    }

    filtered.forEach((e) => addWithParents(e.departmentId));
    setTreeData(buildDepartmentTree(resultList, DEPARTMENT_PARENT_ID) ?? []);
  };

  const handleReset = () => {
    setKeywords('');
    setTreeData(buildDepartmentTree(departmentList, DEPARTMENT_PARENT_ID) ?? []);
  };

  const handleAdd = (parentRecord?: DepartmentVO) => {
    setCurrentRecord({
      departmentId: 0,
      departmentName: '',
      parentId: parentRecord?.departmentId ?? undefined,
    });
    setModalVisible(true);
  };

  const handleEdit = (record: DepartmentVO) => {
    setCurrentRecord(record);
    setModalVisible(true);
  };

  const handleDelete = (departmentId: number) => {
    Modal.confirm({
      title: '提醒',
      content: '确定要删除该部门吗?',
      okText: '删除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        const res = await departmentApi.delete(departmentId);
        if (res.code === 1) {
          message.success('删除成功');
          queryDepartmentTree();
        }
      },
    });
  };

  const columns: ColumnsType<DepartmentVO> = [
    { title: '部门名称', dataIndex: 'departmentName', key: 'departmentName' },
    { title: '负责人', dataIndex: 'managerName', key: 'managerName', width: 100 },
    { title: '排序', dataIndex: 'sort', key: 'sort', width: 100 },
    { title: '创建时间', dataIndex: 'createTime', width: 150 },
    { title: '更新时间', dataIndex: 'updateTime', width: 150 },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 200,
      render: (_: unknown, record: DepartmentVO) => (
        <Space>
          <PrivilegeButton permissionCode="system:department:add" type="link" size="small" onClick={() => handleAdd(record)}>
            添加下级
          </PrivilegeButton>
          <PrivilegeButton permissionCode="system:department:update" type="link" size="small" onClick={() => handleEdit(record)}>
            编辑
          </PrivilegeButton>
          {record.departmentId !== topDepartmentId && (
            <PrivilegeButton
              permissionCode="system:department:delete"
              type="link"
              size="small"
              danger
              onClick={() => handleDelete(record.departmentId)}
            >
              删除
            </PrivilegeButton>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <Form layout="inline" style={{ marginBottom: 16 }}>
        <Form.Item label="部门名称">
          <Input
            style={{ width: 300 }}
            value={keywords}
            onChange={(e) => setKeywords(e.target.value)}
            onPressEnter={handleSearch}
            placeholder="请输入部门名称"
          />
        </Form.Item>
        <Form.Item>
          <Space>
            <PrivilegeButton permissionCode="support:department:query" type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
              查询
            </PrivilegeButton>
            <PrivilegeButton permissionCode="support:department:query" icon={<ReloadOutlined />} onClick={handleReset}>
              重置
            </PrivilegeButton>
            <PrivilegeButton permissionCode="system:department:add" type="primary" icon={<PlusOutlined />} onClick={() => handleAdd()}>
              新建
            </PrivilegeButton>
          </Space>
        </Form.Item>
      </Form>

      <Card size="small" bordered>
        <Table
          size="small"
          bordered
          loading={loading}
          rowKey="departmentId"
          columns={columns}
          dataSource={treeData}
          defaultExpandedRowKeys={defaultExpandedRowKeys}
          pagination={false}
        />
      </Card>

      <DepartmentFormModal
        visible={modalVisible}
        record={currentRecord}
        onCancel={() => setModalVisible(false)}
        onSuccess={() => {
          setModalVisible(false);
          queryDepartmentTree();
        }}
      />
    </div>
  );
};

export default DepartmentList;
