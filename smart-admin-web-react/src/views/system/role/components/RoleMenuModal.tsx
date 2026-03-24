/**
 * Role Menu Permission Modal Component
 * 角色菜單權限分配 Modal 組件
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-23
 */

import React, { useEffect, useState } from 'react';
import { Modal, Tree, message, Alert } from 'antd';
import type { TreeProps, DataNode } from 'antd/es/tree';
import { roleApi } from '@/api/system/roleApi';
import type { RoleVO, RoleMenuUpdateForm } from '../types';
import type { MenuVO } from '@/views/system/menu/types';

interface RoleMenuModalProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess: () => void;
  role?: RoleVO;
}

/**
 * 將 MenuVO 轉換為 Ant Design Tree 所需的 DataNode
 */
function convertMenuToTreeData(menuList: MenuVO[]): DataNode[] {
  return menuList.map((menu) => ({
    key: menu.menuId,
    title: menu.menuName,
    children: menu.children && menu.children.length > 0 ? convertMenuToTreeData(menu.children) : undefined,
  }));
}

export default function RoleMenuModal({ visible, onCancel, onSuccess, role }: RoleMenuModalProps) {
  const [loading, setLoading] = useState(false);
  const [treeData, setTreeData] = useState<DataNode[]>([]);
  const [checkedKeys, setCheckedKeys] = useState<React.Key[]>([]);
  const [halfCheckedKeys, setHalfCheckedKeys] = useState<React.Key[]>([]);
  const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([]);

  /**
   * 加載角色菜單權限數據
   */
  const loadRoleMenu = async () => {
    if (!role) return;

    try {
      setLoading(true);
      const res = await roleApi.getRoleSelectedMenu(role.roleId);
      if (res.ok && res.data) {
        // 轉換菜單樹數據
        const treeNodes = convertMenuToTreeData(res.data.menuTreeList || []);
        setTreeData(treeNodes);

        // 設置已選中的菜單 keys
        setCheckedKeys(res.data.selectedMenuId || []);

        // 自動展開所有節點
        const allKeys = extractAllKeys(res.data.menuTreeList || []);
        setExpandedKeys(allKeys);
      }
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message || '加載菜單權限失敗');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * 提取所有菜單 keys（用於展開）
   */
  const extractAllKeys = (menuList: MenuVO[]): number[] => {
    const keys: number[] = [];
    menuList.forEach((menu) => {
      keys.push(menu.menuId);
      if (menu.children && menu.children.length > 0) {
        keys.push(...extractAllKeys(menu.children));
      }
    });
    return keys;
  };

  /**
   * Tree 選中事件
   */
  const onCheck: TreeProps['onCheck'] = (checkedKeysValue, info) => {
    setCheckedKeys(checkedKeysValue as React.Key[]);
    setHalfCheckedKeys(info.halfCheckedKeys || []);
  };

  /**
   * Tree 展開/收起事件
   */
  const onExpand: TreeProps['onExpand'] = (expandedKeysValue) => {
    setExpandedKeys(expandedKeysValue);
  };

  /**
   * 保存菜單權限
   */
  const handleSubmit = async () => {
    if (!role) return;

    try {
      setLoading(true);

      // 合併選中的 keys 和半選中的 keys（父節點）
      const allSelectedKeys = [...checkedKeys, ...halfCheckedKeys].map((key) => Number(key));

      if (allSelectedKeys.length === 0) {
        message.warning('請至少選擇一個菜單權限');
        return;
      }

      const params: RoleMenuUpdateForm = {
        roleId: role.roleId,
        menuIdList: allSelectedKeys,
      };

      const res = await roleApi.updateRoleMenu(params);
      if (res.ok) {
        message.success('保存成功');
        onSuccess();
      }
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message || '保存失敗');
      }
    } finally {
      setLoading(false);
    }
  };

  /**
   * Modal 關閉處理
   */
  const handleCancel = () => {
    setCheckedKeys([]);
    setHalfCheckedKeys([]);
    setExpandedKeys([]);
    onCancel();
  };

  /**
   * Modal 顯示時加載數據
   */
  useEffect(() => {
    if (visible && role) {
      loadRoleMenu();
    }
  }, [visible, role]);

  return (
    <Modal
      title={`設置角色功能權限 - ${role?.roleName || ''}`}
      open={visible}
      onOk={handleSubmit}
      onCancel={handleCancel}
      confirmLoading={loading}
      width={600}
      destroyOnClose
      bodyStyle={{ maxHeight: '60vh', overflowY: 'auto' }}
    >
      <Alert
        message="提示"
        description="勾選菜單項後，該角色將擁有對應的功能操作權限"
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
      />

      {treeData.length > 0 ? (
        <Tree
          checkable
          defaultExpandAll
          expandedKeys={expandedKeys}
          checkedKeys={checkedKeys}
          onCheck={onCheck}
          onExpand={onExpand}
          treeData={treeData}
          style={{ marginTop: 16 }}
        />
      ) : (
        <div style={{ textAlign: 'center', padding: '40px 0', color: '#999' }}>暫無菜單數據</div>
      )}
    </Modal>
  );
}
