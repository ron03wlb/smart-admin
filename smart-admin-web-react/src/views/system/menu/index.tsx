/**
 * Menu Management Page
 * 菜單管理頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/system/menu/
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import React, { useState, useEffect, useCallback } from 'react';
import {
  Card,
  Input,
  Button,
  Table,
  Space,
  Modal,
  message,
  Typography,
  Select,
  Tag,
  Row,
  Col,
  Form,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  PlusOutlined,
  MoreOutlined,
  DeleteOutlined,
  ExclamationCircleOutlined,
} from '@ant-design/icons';
import type { TableColumnsType } from 'antd';
import { usePrivilege } from '@/hooks/usePrivilege';
import PrivilegeButton from '@/components/PrivilegeButton';
import TableOperator from '@/components/common/TableOperator';
import { menuApi } from '@/api/system/menuApi';
import type { MenuVO, MenuQueryForm, MenuFormData, MenuTypeEnum } from './types';
import {
  MENU_PERMISSION,
  MENU_TYPE_LABELS,
  MENU_TYPE_COLORS,
  MENU_TABLE_COLUMNS_WIDTH,
  MENU_CONSTANTS,
} from '@/constants/system/menuConst';
import MenuFormModal from './components/MenuFormModal';

const { Title } = Typography;
const { Option } = Select;

/**
 * 菜單管理頁面
 */
export default function MenuPage() {
  const hasUpdatePrivilege = usePrivilege(MENU_PERMISSION.UPDATE);
  const hasDeletePrivilege = usePrivilege(MENU_PERMISSION.BATCH_DELETE);

  // ==================== State Management ====================

  const [loading, setLoading] = useState(false);
  const [showAdvancedSearch, setShowAdvancedSearch] = useState(true);
  const [menuList, setMenuList] = useState<MenuVO[]>([]);
  const [tableData, setTableData] = useState<MenuVO[]>([]);
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);
  const [expandedRowKeys, setExpandedRowKeys] = useState<React.Key[]>([]);

  // Query form
  const [queryForm, setQueryForm] = useState<MenuQueryForm>({
    keywords: undefined,
    menuType: undefined,
    disabledFlag: undefined,
    frameFlag: undefined,
    cacheFlag: undefined,
    visibleFlag: undefined,
  });

  // Modal state
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [formInitialData, setFormInitialData] = useState<MenuFormData | undefined>();

  // ==================== Helper Functions ====================

  /**
   * 構建菜單樹
   */
  const buildMenuTree = useCallback((data: MenuVO[], parentId: number): MenuVO[] => {
    const children = data.filter(item => item.parentId === parentId);

    if (children.length === 0) {
      return [];
    }

    return children
      .sort((a, b) => a.sort - b.sort)
      .map(item => {
        const node = { ...item };
        const subChildren = buildMenuTree(data, item.menuId);
        if (subChildren.length > 0) {
          node.children = subChildren;
        }
        return node;
      });
  }, []);

  /**
   * 過濾菜單（根據查詢條件）
   */
  const filterMenuByQueryForm = useCallback((data: MenuVO[], form: MenuQueryForm): MenuVO[] => {
    return data.filter(menu => {
      // 關鍵字搜索
      if (form.keywords) {
        const keyword = form.keywords.toLowerCase();
        const matchKeyword =
          menu.menuName?.toLowerCase().includes(keyword) ||
          menu.path?.toLowerCase().includes(keyword) ||
          menu.component?.toLowerCase().includes(keyword) ||
          menu.webPerms?.toLowerCase().includes(keyword) ||
          menu.apiPerms?.toLowerCase().includes(keyword);

        if (!matchKeyword) return false;
      }

      // 菜單類型篩選
      if (form.menuType !== undefined && menu.menuType !== form.menuType) {
        return false;
      }

      // 禁用狀態篩選
      if (form.disabledFlag !== undefined && menu.disabledFlag !== form.disabledFlag) {
        return false;
      }

      // 外鏈狀態篩選
      if (form.frameFlag !== undefined && menu.frameFlag !== form.frameFlag) {
        return false;
      }

      // 緩存狀態篩選
      if (form.cacheFlag !== undefined && menu.cacheFlag !== form.cacheFlag) {
        return false;
      }

      // 顯示狀態篩選
      if (form.visibleFlag !== undefined && menu.visibleFlag !== form.visibleFlag) {
        return false;
      }

      return true;
    });
  }, []);

  /**
   * 獲取所有節點的 key（用於默認展開）
   */
  const getAllKeys = useCallback((data: MenuVO[]): React.Key[] => {
    const keys: React.Key[] = [];
    const traverse = (nodes: MenuVO[]) => {
      nodes.forEach(node => {
        keys.push(node.menuId);
        if (node.children) {
          traverse(node.children);
        }
      });
    };
    traverse(data);
    return keys;
  }, []);

  // ==================== Data Loading ====================

  /**
   * 查詢菜單列表並構建樹形結構
   */
  const queryMenuList = useCallback(async () => {
    try {
      setLoading(true);
      const response = await menuApi.queryMenu();
      const data = response.data || [];

      setMenuList(data);

      // 過濾並構建樹形數據
      const filteredData = filterMenuByQueryForm(data, queryForm);
      const treeData = buildMenuTree(filteredData, MENU_CONSTANTS.TOP_PARENT_ID);
      setTableData(treeData);

      // 默認展開所有節點
      const allKeys = getAllKeys(treeData);
      setExpandedRowKeys(allKeys);
    } catch (error) {
      message.error('查詢菜單列表失敗');
    } finally {
      setLoading(false);
    }
  }, [queryForm, filterMenuByQueryForm, buildMenuTree, getAllKeys]);

  useEffect(() => {
    queryMenuList();
  }, [queryMenuList]);

  // ==================== Search Operations ====================

  /**
   * 處理搜索
   */
  const handleSearch = () => {
    queryMenuList();
  };

  /**
   * 處理重置
   */
  const handleReset = () => {
    setQueryForm({
      keywords: undefined,
      menuType: undefined,
      disabledFlag: undefined,
      frameFlag: undefined,
      cacheFlag: undefined,
      visibleFlag: undefined,
    });
  };

  useEffect(() => {
    if (Object.values(queryForm).every(v => v === undefined)) {
      queryMenuList();
    }
  }, [queryForm, queryMenuList]);

  // ==================== Table Columns Definition ====================

  const columns: TableColumnsType<MenuVO> = [
    {
      title: '菜單名稱',
      dataIndex: 'menuName',
      key: 'menuName',
      width: MENU_TABLE_COLUMNS_WIDTH.menuName,
      fixed: 'left',
    },
    {
      title: '類型',
      dataIndex: 'menuType',
      key: 'menuType',
      width: MENU_TABLE_COLUMNS_WIDTH.menuType,
      render: (type: MenuTypeEnum) => (
        <Tag color={MENU_TYPE_COLORS[type]}>{MENU_TYPE_LABELS[type]}</Tag>
      ),
    },
    {
      title: '圖標',
      dataIndex: 'icon',
      key: 'icon',
      width: MENU_TABLE_COLUMNS_WIDTH.icon,
      render: icon => (icon ? <span>{icon}</span> : '-'),
    },
    {
      title: '路由',
      dataIndex: 'path',
      key: 'path',
      width: MENU_TABLE_COLUMNS_WIDTH.path,
      render: text => text || '-',
    },
    {
      title: '組件/外鏈',
      dataIndex: 'component',
      key: 'component',
      width: MENU_TABLE_COLUMNS_WIDTH.component,
      render: (_: any, record: MenuVO) => {
        if (record.frameFlag) {
          return record.frameUrl || '-';
        }
        return record.component || '-';
      },
      ellipsis: true,
    },
    {
      title: '前端權限',
      dataIndex: 'webPerms',
      key: 'webPerms',
      width: MENU_TABLE_COLUMNS_WIDTH.webPerms,
      render: text => text || '-',
      ellipsis: true,
    },
    {
      title: '後端權限',
      dataIndex: 'apiPerms',
      key: 'apiPerms',
      width: MENU_TABLE_COLUMNS_WIDTH.apiPerms,
      render: text => text || '-',
      ellipsis: true,
    },
    {
      title: '排序',
      dataIndex: 'sort',
      key: 'sort',
      width: MENU_TABLE_COLUMNS_WIDTH.sort,
    },
    {
      title: '操作',
      key: 'operate',
      fixed: 'right',
      width: MENU_TABLE_COLUMNS_WIDTH.operate,
      render: (_: any, record: MenuVO) => (
        <Space size="small">
          {record.menuType !== 3 && hasUpdatePrivilege && (
            <Button type="link" size="small" onClick={() => handleAddSub(record)}>
              添加下級
            </Button>
          )}

          {hasUpdatePrivilege && (
            <Button type="link" size="small" onClick={() => handleEdit(record)}>
              編輯
            </Button>
          )}

          {hasDeletePrivilege && (
            <Button type="link" size="small" danger onClick={() => handleSingleDelete(record)}>
              刪除
            </Button>
          )}
        </Space>
      ),
    },
  ];

  // ==================== CRUD Operations ====================

  /**
   * 處理新增菜單
   */
  const handleAdd = () => {
    setFormInitialData({
      parentId: MENU_CONSTANTS.TOP_PARENT_ID,
      menuType: 1, // 默認為目錄
      sort: 0,
      visibleFlag: true,
      cacheFlag: false,
      disabledFlag: false,
      frameFlag: false,
    });
    setFormModalVisible(true);
  };

  /**
   * 處理添加下級菜單
   */
  const handleAddSub = (parent: MenuVO) => {
    const menuType = parent.menuType === 1 ? 2 : 3; // 目錄下添加菜單，菜單下添加功能點

    setFormInitialData({
      parentId: parent.menuId,
      menuType,
      contextMenuId: parent.menuType === 2 ? parent.menuId : undefined,
      sort: 0,
      visibleFlag: true,
      cacheFlag: false,
      disabledFlag: false,
      frameFlag: false,
    });
    setFormModalVisible(true);
  };

  /**
   * 處理編輯菜單
   */
  const handleEdit = (record: MenuVO) => {
    setFormInitialData(record as MenuFormData);
    setFormModalVisible(true);
  };

  /**
   * 表單提交成功回調
   */
  const handleFormSuccess = () => {
    setFormModalVisible(false);
    queryMenuList(); // 刷新列表
  };

  /**
   * 處理單個刪除
   */
  const handleSingleDelete = (record: MenuVO) => {
    confirmBatchDelete([record]);
  };

  /**
   * 處理批量刪除
   */
  const handleBatchDelete = () => {
    if (selectedRowKeys.length === 0) {
      message.warning('請選擇要刪除的菜單');
      return;
    }

    const selectedMenus = menuList.filter(menu => selectedRowKeys.includes(menu.menuId));
    confirmBatchDelete(selectedMenus);
  };

  /**
   * 確認批量刪除
   */
  const confirmBatchDelete = (menuArray: MenuVO[]) => {
    const menuNames = menuArray.map(menu => menu.menuName).join('、');

    Modal.confirm({
      title: '確認刪除',
      icon: <ExclamationCircleOutlined />,
      content: `確定要刪除如下菜單嗎？${menuNames}`,
      okText: '刪除',
      okType: 'danger',
      cancelText: '取消',
      onOk: async () => {
        try {
          const menuIdList = menuArray.map(menu => menu.menuId);
          await menuApi.batchDeleteMenu(menuIdList);
          message.success('刪除成功');
          setSelectedRowKeys([]);
          queryMenuList();
        } catch (error) {
          message.error('刪除失敗');
        }
      },
    });
  };

  // ==================== Row Selection ====================

  const rowSelection = {
    selectedRowKeys,
    onChange: (keys: React.Key[]) => setSelectedRowKeys(keys),
  };

  // ==================== Render ====================

  return (
    <div style={{ padding: '24px' }}>
      <Card>
        {/* Header */}
        <div style={{ marginBottom: 16 }}>
          <Title level={5} style={{ marginBottom: 16 }}>
            菜單管理
          </Title>

          {/* Search Bar */}
          <Form layout="inline" style={{ marginBottom: 16 }}>
            <Row gutter={16} style={{ width: '100%' }}>
              <Col>
                <Form.Item label="關鍵字">
                  <Input
                    placeholder="菜單名稱/路由地址/組件路徑/權限字符串"
                    style={{ width: 300 }}
                    value={queryForm.keywords}
                    onChange={e => setQueryForm({ ...queryForm, keywords: e.target.value })}
                  />
                </Form.Item>
              </Col>

              <Col>
                <Form.Item label="類型">
                  <Select
                    placeholder="請選擇類型"
                    style={{ width: 120 }}
                    value={queryForm.menuType}
                    onChange={value => setQueryForm({ ...queryForm, menuType: value })}
                    allowClear
                  >
                    {Object.entries(MENU_TYPE_LABELS).map(([key, label]) => (
                      <Option key={key} value={Number(key)}>
                        {label}
                      </Option>
                    ))}
                  </Select>
                </Form.Item>
              </Col>

              <Col>
                <Form.Item label="禁用">
                  <Select
                    placeholder="請選擇"
                    style={{ width: 120 }}
                    value={queryForm.disabledFlag}
                    onChange={value => setQueryForm({ ...queryForm, disabledFlag: value })}
                    allowClear
                  >
                    <Option value={false}>否</Option>
                    <Option value={true}>是</Option>
                  </Select>
                </Form.Item>
              </Col>

              <Col>
                <Space>
                  <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
                    查詢
                  </Button>
                  <Button icon={<ReloadOutlined />} onClick={handleReset}>
                    重置
                  </Button>
                  <Button
                    icon={<MoreOutlined />}
                    onClick={() => setShowAdvancedSearch(!showAdvancedSearch)}
                  >
                    {showAdvancedSearch ? '收起' : '展開'}
                  </Button>
                </Space>
              </Col>
            </Row>

            {/* Advanced Search */}
            {showAdvancedSearch && (
              <Row gutter={16} style={{ width: '100%', marginTop: 16 }}>
                <Col>
                  <Form.Item label="外鏈">
                    <Select
                      placeholder="請選擇"
                      style={{ width: 120 }}
                      value={queryForm.frameFlag}
                      onChange={value => setQueryForm({ ...queryForm, frameFlag: value })}
                      allowClear
                    >
                      <Option value={false}>否</Option>
                      <Option value={true}>是</Option>
                    </Select>
                  </Form.Item>
                </Col>

                <Col>
                  <Form.Item label="緩存">
                    <Select
                      placeholder="請選擇"
                      style={{ width: 120 }}
                      value={queryForm.cacheFlag}
                      onChange={value => setQueryForm({ ...queryForm, cacheFlag: value })}
                      allowClear
                    >
                      <Option value={false}>否</Option>
                      <Option value={true}>是</Option>
                    </Select>
                  </Form.Item>
                </Col>

                <Col>
                  <Form.Item label="顯示">
                    <Select
                      placeholder="請選擇"
                      style={{ width: 120 }}
                      value={queryForm.visibleFlag}
                      onChange={value => setQueryForm({ ...queryForm, visibleFlag: value })}
                      allowClear
                    >
                      <Option value={false}>否</Option>
                      <Option value={true}>是</Option>
                    </Select>
                  </Form.Item>
                </Col>
              </Row>
            )}
          </Form>

          {/* Table Operator */}
          <TableOperator
            buttons={[
              {
                type: 'add',
                text: '添加菜單',
                onClick: handleAdd,
                privilege: MENU_PERMISSION.ADD,
              },
              {
                type: 'delete',
                text: '批量刪除',
                onClick: handleBatchDelete,
                privilege: MENU_PERMISSION.BATCH_DELETE,
                disabled: selectedRowKeys.length === 0,
              },
            ]}
            showRefresh={true}
            onRefresh={queryMenuList}
            showColumnSetting={true}
            onColumnSettingClick={() => {
              message.info('列設置功能開發中');
            }}
          />
        </div>

        {/* Table */}
        <Table
          rowKey="menuId"
          columns={columns}
          dataSource={tableData}
          loading={loading}
          pagination={false}
          rowSelection={rowSelection}
          expandable={{
            expandedRowKeys,
            onExpandedRowsChange: keys => setExpandedRowKeys([...keys]),
          }}
          bordered
          size="small"
          scroll={{ x: 1800, y: 800 }}
        />
      </Card>

      {/* 菜單表單 Modal */}
      <MenuFormModal
        visible={formModalVisible}
        onCancel={() => setFormModalVisible(false)}
        onSuccess={handleFormSuccess}
        initialData={formInitialData}
      />
    </div>
  );
}
