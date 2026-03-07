/**
 * Menu Operate Drawer
 *
 * Corresponds to Vue's menu/components/menu-operate-modal.vue (297L)
 * Drawer-based form for add/edit menu. Supports 3 menu types with dynamic fields.
 */
import React, { useEffect } from 'react';
import { Drawer, Form, Input, InputNumber, Radio, Switch, Button, Space, message } from 'antd';
import { menuApi } from '@/api/system/menu-api';
import type { MenuAddForm } from '@/api/system/menu-api';
import type { MenuItem } from '@/types/user.types';
import { MENU_TYPE_ENUM, MENU_DEFAULT_PARENT_ID } from '@/constants/system/menu-const';
import MenuTreeSelect from '@/components/system/menu-tree-select/MenuTreeSelect';

interface MenuOperateDrawerProps {
  open: boolean;
  menu?: MenuItem;
  parentForAdd?: { parentId: string; menuType: number };
  onClose: () => void;
  onSuccess: () => void;
}

const MenuOperateDrawer: React.FC<MenuOperateDrawerProps> = ({
  open,
  menu,
  parentForAdd,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const isEdit = !!menu?.menuId;
  const menuType = Form.useWatch('menuType', form);

  useEffect(() => {
    if (open) {
      form.resetFields();
      if (menu) {
        form.setFieldsValue({
          ...menu,
          menuType: Number(menu.menuType),
          parentId: menu.parentId && menu.parentId !== String(MENU_DEFAULT_PARENT_ID) ? menu.parentId : undefined,
          visibleFlag: menu.visibleFlag ?? true,
          disabledFlag: menu.disabledFlag ?? false,
          cacheFlag: menu.cacheFlag ?? true,
          frameFlag: menu.frameFlag ?? false,
          sort: menu.sort ?? 0,
        });
      } else if (parentForAdd) {
        form.setFieldsValue({
          menuType: parentForAdd.menuType,
          parentId: parentForAdd.parentId,
          visibleFlag: true,
          disabledFlag: false,
          cacheFlag: true,
          frameFlag: false,
          sort: 0,
        });
      } else {
        form.setFieldsValue({
          menuType: MENU_TYPE_ENUM.CATALOG.value,
          visibleFlag: true,
          disabledFlag: false,
          cacheFlag: true,
          frameFlag: false,
          sort: 0,
        });
      }
    }
  }, [open, menu, parentForAdd, form]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    const data: MenuAddForm = {
      ...values,
      parentId: values.parentId ?? MENU_DEFAULT_PARENT_ID,
    };

    if (isEdit) {
      await menuApi.update({ ...data, menuId: Number(menu!.menuId) });
    } else {
      await menuApi.add(data);
    }
    message.success(`${isEdit ? '编辑' : '添加'}成功`);
    onSuccess();
  };

  const isCatalogOrMenu = menuType === MENU_TYPE_ENUM.CATALOG.value || menuType === MENU_TYPE_ENUM.MENU.value;
  const isPoints = menuType === MENU_TYPE_ENUM.POINTS.value;

  return (
    <Drawer
      title={isEdit ? '编辑菜单' : '添加菜单'}
      open={open}
      width={600}
      onClose={onClose}
      extra={
        <Space>
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" onClick={handleSubmit}>提交</Button>
        </Space>
      }
    >
      <Form form={form} labelCol={{ span: 6 }} wrapperCol={{ span: 16 }}>
        <Form.Item label="菜单类型" name="menuType" rules={[{ required: true }]}>
          <Radio.Group>
            <Radio value={MENU_TYPE_ENUM.CATALOG.value}>目录</Radio>
            <Radio value={MENU_TYPE_ENUM.MENU.value}>菜单</Radio>
            <Radio value={MENU_TYPE_ENUM.POINTS.value}>功能点</Radio>
          </Radio.Group>
        </Form.Item>

        <Form.Item
          label="菜单名称"
          name="menuName"
          rules={[{ required: true, message: '请输入菜单名称' }, { max: 20, message: '最多20个字符' }]}
        >
          <Input placeholder="请输入菜单名称" />
        </Form.Item>

        <Form.Item label="上级菜单" name="parentId">
          <MenuTreeSelect placeholder="不选则为顶级菜单" />
        </Form.Item>

        {isCatalogOrMenu && (
          <>
            <Form.Item label="图标" name="icon">
              <Input placeholder="请输入图标名称" />
            </Form.Item>
            <Form.Item label="路由路径" name="path">
              <Input placeholder="请输入路由路径" />
            </Form.Item>
          </>
        )}

        {menuType === MENU_TYPE_ENUM.MENU.value && (
          <>
            <Form.Item label="组件路径" name="component">
              <Input placeholder="请输入组件路径" />
            </Form.Item>
            <Form.Item label="是否缓存" name="cacheFlag" valuePropName="checked">
              <Switch />
            </Form.Item>
            <Form.Item label="是否外链" name="frameFlag" valuePropName="checked">
              <Switch />
            </Form.Item>
          </>
        )}

        {isPoints && (
          <>
            <Form.Item label="前端权限" name="webPerms">
              <Input placeholder="前端权限标识" />
            </Form.Item>
            <Form.Item label="后端权限" name="apiPerms">
              <Input placeholder="后端权限标识，多个用逗号分隔" />
            </Form.Item>
          </>
        )}

        <Form.Item label="排序" name="sort">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>

        <Form.Item label="是否显示" name="visibleFlag" valuePropName="checked">
          <Switch />
        </Form.Item>

        <Form.Item label="是否禁用" name="disabledFlag" valuePropName="checked">
          <Switch />
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default MenuOperateDrawer;
