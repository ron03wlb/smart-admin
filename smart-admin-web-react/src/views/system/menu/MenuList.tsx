/**
 * Menu List Page
 *
 * Corresponds to Vue's system/menu/menu-list.vue (278L)
 * Tree table for menu management with search, CRUD operations.
 */
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Card, Table, Input, Button, Space, Tag, Modal, message, Select } from 'antd';
import { PlusOutlined, DeleteOutlined } from '@ant-design/icons';
import { menuApi } from '@/api/system/menu-api';
import type { MenuItem } from '@/types/user.types';
import { MENU_TYPE_ENUM } from '@/constants/system/menu-const';
import type { ColumnsType } from 'antd/es/table';
import MenuOperateDrawer from './MenuOperateDrawer';

/** Build tree from flat menu list */
function buildMenuTree(menuList: MenuItem[]): MenuItem[] {
  const idSet = new Set(menuList.map((m) => m.menuId));
  const map = new Map<string | number, MenuItem & { children?: MenuItem[] }>();

  for (const item of menuList) {
    map.set(item.menuId, { ...item, children: [] });
  }

  const roots: MenuItem[] = [];
  for (const item of menuList) {
    const node = map.get(item.menuId)!;
    if (item.parentId && idSet.has(item.parentId)) {
      map.get(item.parentId)!.children!.push(node);
    } else {
      roots.push(node);
    }
  }
  return roots;
}

/** Filter menus by keywords */
function filterMenus(menuList: MenuItem[], keywords: string, menuType?: number): MenuItem[] {
  if (!keywords && menuType == null) return menuList;
  return menuList.filter((menu) => {
    if (menuType != null && String(menu.menuType) !== String(menuType)) return false;
    if (keywords) {
      const kw = keywords.toLowerCase();
      const fields = [menu.menuName, menu.path, menu.component, menu.apiPerms, menu.webPerms];
      return fields.some((f) => f && f.toLowerCase().includes(kw));
    }
    return true;
  });
}

const menuTypeTagMap: Record<string, { color: string; text: string }> = {
  [MENU_TYPE_ENUM.CATALOG.value]: { color: 'blue', text: '目录' },
  [MENU_TYPE_ENUM.MENU.value]: { color: 'green', text: '菜单' },
  [MENU_TYPE_ENUM.POINTS.value]: { color: 'orange', text: '功能点' },
};

const MenuList: React.FC = () => {
  const [allMenus, setAllMenus] = useState<MenuItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [keywords, setKeywords] = useState('');
  const [menuType, setMenuType] = useState<number | undefined>();
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([]);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [currentMenu, setCurrentMenu] = useState<MenuItem | undefined>();
  const [parentForAdd, setParentForAdd] = useState<{ parentId: string; menuType: number } | undefined>();

  const queryMenus = useCallback(async () => {
    setLoading(true);
    try {
      const res = await menuApi.query();
      if (res.code === 1 && res.data) {
        setAllMenus(res.data);
      }
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    queryMenus();
  }, [queryMenus]);

  const treeData = useMemo(() => {
    const filtered = filterMenus(allMenus, keywords, menuType);
    return buildMenuTree(filtered);
  }, [allMenus, keywords, menuType]);

  const handleAdd = () => {
    setCurrentMenu(undefined);
    setParentForAdd(undefined);
    setDrawerOpen(true);
  };

  const handleAddSub = (parent: MenuItem) => {
    setCurrentMenu(undefined);
    const childType = Number(parent.menuType) === MENU_TYPE_ENUM.MENU.value
      ? MENU_TYPE_ENUM.POINTS.value
      : MENU_TYPE_ENUM.MENU.value;
    setParentForAdd({ parentId: String(parent.menuId), menuType: childType });
    setDrawerOpen(true);
  };

  const handleEdit = (record: MenuItem) => {
    setCurrentMenu(record);
    setParentForAdd(undefined);
    setDrawerOpen(true);
  };

  const handleDelete = (menuId: string | number) => {
    Modal.confirm({
      title: '提示',
      content: '确定要删除该菜单么？',
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      async onOk() {
        await menuApi.batchDelete([Number(menuId)]);
        message.success('删除成功');
        queryMenus();
      },
    });
  };

  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的菜单');
      return;
    }
    Modal.confirm({
      title: '提示',
      content: `确定要删除选中的 ${selectedRowKeys.length} 个菜单么？`,
      okText: '确定',
      okType: 'danger',
      cancelText: '取消',
      async onOk() {
        await menuApi.batchDelete(selectedRowKeys.map(Number));
        message.success('删除成功');
        setSelectedRowKeys([]);
        queryMenus();
      },
    });
  };

  const handleReset = () => {
    setKeywords('');
    setMenuType(undefined);
  };

  const handleDrawerSuccess = () => {
    setDrawerOpen(false);
    queryMenus();
  };

  const columns: ColumnsType<MenuItem> = [
    { title: '名称', dataIndex: 'menuName', width: 220 },
    {
      title: '类型',
      dataIndex: 'menuType',
      width: 100,
      align: 'center',
      render: (val) => {
        const info = menuTypeTagMap[val];
        return info ? <Tag color={info.color}>{info.text}</Tag> : val;
      },
    },
    { title: '路径', dataIndex: 'path', ellipsis: true },
    { title: '组件', dataIndex: 'component', ellipsis: true },
    { title: '权限', dataIndex: 'apiPerms', ellipsis: true },
    { title: '排序', dataIndex: 'sort', width: 80 },
    {
      title: '操作',
      width: 200,
      align: 'center',
      render: (_, record) => (
        <Space size="small">
          {String(record.menuType) !== String(MENU_TYPE_ENUM.POINTS.value) && (
            <a onClick={() => handleAddSub(record)}>添加子级</a>
          )}
          <a onClick={() => handleEdit(record)}>编辑</a>
          <a style={{ color: '#ff4d4f' }} onClick={() => handleDelete(record.menuId)}>删除</a>
        </Space>
      ),
    },
  ];

  return (
    <Card>
      <Space style={{ marginBottom: 16 }} wrap>
        <Input
          style={{ width: 200 }}
          value={keywords}
          onChange={(e) => setKeywords(e.target.value)}
          placeholder="名称/路径/组件/权限"
          allowClear
        />
        <Select
          style={{ width: 120 }}
          value={menuType}
          onChange={setMenuType}
          placeholder="菜单类型"
          allowClear
          options={[
            { label: '目录', value: MENU_TYPE_ENUM.CATALOG.value },
            { label: '菜单', value: MENU_TYPE_ENUM.MENU.value },
            { label: '功能点', value: MENU_TYPE_ENUM.POINTS.value },
          ]}
        />
        <Button type="primary" onClick={queryMenus}>查询</Button>
        <Button onClick={handleReset}>重置</Button>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>新建</Button>
        <Button danger icon={<DeleteOutlined />} onClick={handleBatchDelete}>批量删除</Button>
      </Space>

      <Table
        rowKey="menuId"
        columns={columns}
        dataSource={treeData}
        loading={loading}
        size="small"
        pagination={false}
        defaultExpandAllRows
        rowSelection={{
          selectedRowKeys,
          onChange: (keys) => setSelectedRowKeys(keys as string[]),
        }}
      />

      <MenuOperateDrawer
        open={drawerOpen}
        menu={currentMenu}
        parentForAdd={parentForAdd}
        onClose={() => setDrawerOpen(false)}
        onSuccess={handleDrawerSuccess}
      />
    </Card>
  );
};

export default MenuList;
