/**
 * Role Permission Tree
 *
 * Corresponds to Vue's role/components/role-tree/ (4 files merged into 1)
 * Uses Ant Design Tree with checkable to manage menu/permission assignments.
 */
import React, { useCallback, useContext, useEffect, useState } from 'react';
import { Tree, Button, message, Space } from 'antd';
import { roleMenuApi } from '@/api/system/role-menu-api';
import type { MenuTreeNode } from '@/types/role.types';
import { RoleContext } from '../index';
import type { Key } from 'react';

const RoleTree: React.FC = () => {
  const { selectedRoleId } = useContext(RoleContext);
  const [treeData, setTreeData] = useState<MenuTreeNode[]>([]);
  const [checkedKeys, setCheckedKeys] = useState<string[]>([]);
  const [halfCheckedKeys, setHalfCheckedKeys] = useState<string[]>([]);
  const [loading, setLoading] = useState(false);

  const loadRoleMenu = useCallback(async () => {
    if (!selectedRoleId) return;
    const res = await roleMenuApi.getRoleSelectedMenu(selectedRoleId);
    if (res.code === 1 && res.data) {
      setTreeData(res.data.menuTreeList || []);
      setCheckedKeys(res.data.selectedMenuId || []);
      setHalfCheckedKeys([]);
    }
  }, [selectedRoleId]);

  useEffect(() => {
    loadRoleMenu();
  }, [loadRoleMenu]);

  const handleCheck = (
    checked: Key[] | { checked: Key[]; halfChecked: Key[] },
  ) => {
    if (Array.isArray(checked)) {
      setCheckedKeys(checked as string[]);
    } else {
      setCheckedKeys(checked.checked as string[]);
      setHalfCheckedKeys(checked.halfChecked as string[]);
    }
  };

  const handleSave = async () => {
    if (!selectedRoleId) return;
    const allKeys = [...checkedKeys, ...halfCheckedKeys];
    if (allKeys.length === 0) {
      message.error('还未选择任何权限');
      return;
    }
    setLoading(true);
    try {
      await roleMenuApi.updateRoleMenu({
        roleId: selectedRoleId,
        menuIdList: allKeys,
      });
      message.success('保存成功');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <Space style={{ marginBottom: 12 }}>
        <span style={{ color: '#999' }}>设置角色对应的功能操作、后台管理权限</span>
        {selectedRoleId && (
          <Button type="primary" onClick={handleSave} loading={loading}>
            保存
          </Button>
        )}
      </Space>
      <Tree
        checkable
        checkStrictly
        checkedKeys={{ checked: checkedKeys, halfChecked: halfCheckedKeys }}
        onCheck={handleCheck}
        treeData={treeData as any}
        fieldNames={{ title: 'menuName', key: 'menuId', children: 'children' }}
        defaultExpandAll
        style={{ maxHeight: 600, overflow: 'auto' }}
      />
    </div>
  );
};

export default RoleTree;
