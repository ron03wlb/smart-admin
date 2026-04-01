/**
 * Notice Visible Range Modal
 *
 * Corresponds to Vue's business/oa/notice/components/notice-form-visible-modal.vue
 * and its sub-components:
 *   - notice-form-visible-transfer-employee.vue
 *   - notice-form-visible-transfer-department.vue
 *
 * Allows selecting employees and departments for partial-visibility notices.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Modal, Tabs, Tree, Spin, Empty } from 'antd';
import { UserOutlined, CheckCircleFilled, CloseCircleOutlined } from '@ant-design/icons';
import { departmentApi } from '@/api/system/department-api';
import { employeeApi } from '@/api/system/employee-api';
import type { DepartmentVO } from '@/types/department.types';
import type { EmployeeVO } from '@/api/system/employee-types';

// Visible range data types (matches Vue NOTICE_VISIBLE_RANGE_DATA_TYPE_ENUM)
export const VISIBLE_DATA_TYPE = {
  EMPLOYEE: 1,
  DEPARTMENT: 2,
} as const;

export interface VisibleRangeItem {
  dataId: number;
  dataName: string;
  dataType: number;
}

interface TreeNode {
  key: string;
  title: React.ReactNode;
  departmentId?: number;
  employeeId?: number;
  dataType: number;
  name: string;
  children?: TreeNode[];
}

interface Props {
  open: boolean;
  visibleRangeList: VisibleRangeItem[];
  onClose: () => void;
  onConfirm: (selected: VisibleRangeItem[]) => void;
}

// ==================== Employee Transfer Panel ====================

interface EmployeePanelProps {
  selected: VisibleRangeItem[];
  onChange: (items: VisibleRangeItem[]) => void;
}

const EmployeePanel: React.FC<EmployeePanelProps> = ({ selected, onChange }) => {
  const [treeData, setTreeData] = useState<TreeNode[]>([]);
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);
  const [loading, setLoading] = useState(false);

  const selectedIds = selected.map((item) => item.dataId);

  const buildTree = useCallback(
    async (departments: DepartmentVO[], employees: EmployeeVO[]): Promise<TreeNode[]> => {
      const buildNode = (dept: DepartmentVO): TreeNode => {
        const empChildren: TreeNode[] = employees
          .filter((e) => e.departmentId === dept.departmentId)
          .map((e) => ({
            key: `employee_${e.employeeId}`,
            title: e.employeeName,
            employeeId: e.employeeId,
            dataType: VISIBLE_DATA_TYPE.EMPLOYEE,
            name: e.employeeName,
          }));

        const deptChildren: TreeNode[] = (dept.children || []).map(buildNode);

        return {
          key: `department_${dept.departmentId}`,
          title: dept.departmentName,
          departmentId: dept.departmentId,
          dataType: VISIBLE_DATA_TYPE.DEPARTMENT,
          name: dept.departmentName,
          children: [...deptChildren, ...empChildren],
        };
      };
      return departments.map(buildNode);
    },
    []
  );

  useEffect(() => {
    setLoading(true);
    Promise.all([departmentApi.treeList(), employeeApi.queryAll()])
      .then(async ([deptRes, empRes]) => {
        if (deptRes.code === 1 && empRes.code === 1) {
          const nodes = await buildTree(deptRes.data || [], empRes.data || []);
          setTreeData(nodes);
          if (nodes.length > 0) {
            setExpandedKeys([nodes[0].key]);
          }
        }
      })
      .finally(() => setLoading(false));
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleAdd = (node: TreeNode) => {
    if (node.dataType !== VISIBLE_DATA_TYPE.EMPLOYEE || !node.employeeId) return;
    if (selectedIds.includes(node.employeeId)) return;
    onChange([...selected, { dataId: node.employeeId, dataName: node.name, dataType: VISIBLE_DATA_TYPE.EMPLOYEE }]);
  };

  const handleRemove = (dataId: number) => {
    onChange(selected.filter((item) => item.dataId !== dataId));
  };

  const renderTitle = (node: TreeNode) => {
    if (node.dataType !== VISIBLE_DATA_TYPE.EMPLOYEE) {
      return <span>{node.name}</span>;
    }
    const isSelected = selectedIds.includes(node.employeeId!);
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingRight: 8, height: 28 }}>
        <span style={{ flex: 1, marginRight: 8, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          <UserOutlined style={{ marginRight: 4, fontSize: 12 }} />
          {node.name}
        </span>
        <CheckCircleFilled
          style={{ color: isSelected ? '#1890ff' : '#d9d9d9', cursor: isSelected ? 'default' : 'pointer' }}
          onClick={() => handleAdd(node)}
        />
      </div>
    );
  };

  const treeDataWithTitles = (nodes: TreeNode[]): any[] =>
    nodes.map((n) => ({
      ...n,
      title: renderTitle(n),
      children: n.children ? treeDataWithTitles(n.children) : undefined,
    }));

  if (loading) return <Spin style={{ display: 'block', textAlign: 'center', padding: 40 }} />;

  return (
    <div style={{ display: 'flex', gap: 12 }}>
      <div style={{ flex: 1, height: 400, border: '1px solid #d9d9d9', overflow: 'auto', padding: 8 }}>
        {treeData.length === 0 ? (
          <Empty description="暂无数据" />
        ) : (
          <Tree
            treeData={treeDataWithTitles(treeData)}
            expandedKeys={expandedKeys}
            onExpand={(keys) => setExpandedKeys(keys)}
            selectable={false}
            blockNode
          />
        )}
      </div>
      <div style={{ flex: 1, height: 400, border: '1px solid #d9d9d9', overflow: 'auto', padding: 8 }}>
        {selected.length === 0 ? (
          <Empty description="未选择员工" style={{ marginTop: 40 }} />
        ) : (
          selected.map((item) => (
            <div key={item.dataId} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: 32, padding: '0 8px' }}>
              <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item.dataName}</span>
              <CloseCircleOutlined style={{ color: '#ff4d4f', cursor: 'pointer' }} onClick={() => handleRemove(item.dataId)} />
            </div>
          ))
        )}
      </div>
    </div>
  );
};

// ==================== Department Transfer Panel ====================

interface DeptPanelProps {
  selected: VisibleRangeItem[];
  onChange: (items: VisibleRangeItem[]) => void;
}

const DepartmentPanel: React.FC<DeptPanelProps> = ({ selected, onChange }) => {
  const [treeData, setTreeData] = useState<DepartmentVO[]>([]);
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);
  const [loading, setLoading] = useState(false);

  const selectedIds = selected.map((item) => item.dataId);

  useEffect(() => {
    setLoading(true);
    departmentApi
      .treeList()
      .then((res) => {
        if (res.code === 1 && res.data) {
          setTreeData(res.data);
          if (res.data.length > 0) {
            setExpandedKeys([res.data[0].departmentId]);
          }
        }
      })
      .finally(() => setLoading(false));
  }, []);

  const handleAdd = (dept: DepartmentVO) => {
    if (selectedIds.includes(dept.departmentId)) return;
    onChange([...selected, { dataId: dept.departmentId, dataName: dept.departmentName, dataType: VISIBLE_DATA_TYPE.DEPARTMENT }]);
  };

  const handleRemove = (dataId: number) => {
    onChange(selected.filter((item) => item.dataId !== dataId));
  };

  const buildTreeNodes = (depts: DepartmentVO[]): any[] =>
    depts.map((dept) => {
      const isSelected = selectedIds.includes(dept.departmentId);
      return {
        key: dept.departmentId,
        title: (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingRight: 8, height: 28 }}>
            <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', marginRight: 8 }}>
              {dept.departmentName}
            </span>
            <CheckCircleFilled
              style={{ color: isSelected ? '#1890ff' : '#d9d9d9', cursor: isSelected ? 'default' : 'pointer' }}
              onClick={() => handleAdd(dept)}
            />
          </div>
        ),
        children: dept.children ? buildTreeNodes(dept.children) : undefined,
      };
    });

  if (loading) return <Spin style={{ display: 'block', textAlign: 'center', padding: 40 }} />;

  return (
    <div style={{ display: 'flex', gap: 12 }}>
      <div style={{ flex: 1, height: 400, border: '1px solid #d9d9d9', overflow: 'auto', padding: 8 }}>
        {treeData.length === 0 ? (
          <Empty description="暂无数据" />
        ) : (
          <Tree
            treeData={buildTreeNodes(treeData)}
            expandedKeys={expandedKeys}
            onExpand={(keys) => setExpandedKeys(keys)}
            selectable={false}
            blockNode
          />
        )}
      </div>
      <div style={{ flex: 1, height: 400, border: '1px solid #d9d9d9', overflow: 'auto', padding: 8 }}>
        {selected.length === 0 ? (
          <Empty description="未选择部门" style={{ marginTop: 40 }} />
        ) : (
          selected.map((item) => (
            <div key={item.dataId} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: 32, padding: '0 8px' }}>
              <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item.dataName}</span>
              <CloseCircleOutlined style={{ color: '#ff4d4f', cursor: 'pointer' }} onClick={() => handleRemove(item.dataId)} />
            </div>
          ))
        )}
      </div>
    </div>
  );
};

// ==================== Main Modal ====================

const NoticeVisibleRangeModal: React.FC<Props> = ({ open, visibleRangeList, onClose, onConfirm }) => {
  const [employeeSelected, setEmployeeSelected] = useState<VisibleRangeItem[]>([]);
  const [departmentSelected, setDepartmentSelected] = useState<VisibleRangeItem[]>([]);

  useEffect(() => {
    if (open) {
      setEmployeeSelected(visibleRangeList.filter((item) => item.dataType === VISIBLE_DATA_TYPE.EMPLOYEE));
      setDepartmentSelected(visibleRangeList.filter((item) => item.dataType === VISIBLE_DATA_TYPE.DEPARTMENT));
    }
  }, [open, visibleRangeList]);

  const handleOk = () => {
    onConfirm([...employeeSelected, ...departmentSelected]);
    onClose();
  };

  return (
    <Modal
      title="选择可见范围"
      open={open}
      onOk={handleOk}
      onCancel={onClose}
      width={780}
      maskClosable={false}
      destroyOnClose={false}
    >
      <Tabs
        items={[
          {
            key: 'employee',
            label: `选择员工${employeeSelected.length > 0 ? ` (${employeeSelected.length})` : ''}`,
            children: (
              <EmployeePanel
                selected={employeeSelected}
                onChange={setEmployeeSelected}
              />
            ),
          },
          {
            key: 'department',
            label: `选择部门${departmentSelected.length > 0 ? ` (${departmentSelected.length})` : ''}`,
            children: (
              <DepartmentPanel
                selected={departmentSelected}
                onChange={setDepartmentSelected}
              />
            ),
          },
        ]}
      />
    </Modal>
  );
};

export default NoticeVisibleRangeModal;
