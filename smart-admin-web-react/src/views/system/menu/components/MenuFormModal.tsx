/**
 * Menu Form Modal Component
 * 菜單表單 Modal 組件
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import React, { useEffect, useState } from 'react';
import { Drawer, Form, Input, InputNumber, Radio, Switch, message } from 'antd';
import { useModal } from '@/hooks/useModal';
import { menuApi } from '@/api/system/menuApi';
import type { MenuFormData, MenuAddForm, MenuUpdateForm } from '../types';
import { MenuTypeEnum, PermsTypeEnum } from '../types';
import {
  MENU_VALIDATION,
  MENU_CONSTANTS,
  MENU_TYPE_LABELS,
  PERMS_TYPE_LABELS,
} from '@/constants/system/menuConst';
import MenuTreeSelect from './MenuTreeSelect';
import IconSelect from './IconSelect';

interface MenuFormModalProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess: () => void;
  initialData?: MenuFormData;
}

/**
 * 菜單表單 Modal（使用 Drawer）
 */
export const MenuFormModal: React.FC<MenuFormModalProps> = ({
  visible,
  onCancel,
  onSuccess,
  initialData,
}) => {
  const [form] = Form.useForm();
  const [menuType, setMenuType] = useState<MenuTypeEnum>(MenuTypeEnum.CATALOG);

  // 使用 useModal hook 判斷新增/編輯模式
  const { isEdit } = useModal({
    defaultFormData: initialData,
  });

  // ==================== Form Initialization ====================

  useEffect(() => {
    if (visible) {
      if (initialData) {
        form.setFieldsValue(initialData);
        setMenuType(initialData.menuType || MenuTypeEnum.CATALOG);
      } else {
        form.resetFields();
        setMenuType(MenuTypeEnum.CATALOG);
      }
    }
  }, [visible, initialData, form]);

  // ==================== Form Validation Rules ====================

  /**
   * 權限格式正則：module:resource:action
   * 例如: system:menu:add
   */
  const PERMISSION_PATTERN = /^[a-z0-9]+:[a-z0-9]+:[a-z0-9]+$/;

  /**
   * 多個權限格式正則（逗號分隔）
   * 例如: system:menu:add,system:menu:update
   */
  const MULTI_PERMISSION_PATTERN = /^[a-z0-9]+:[a-z0-9]+:[a-z0-9]+(,[a-z0-9]+:[a-z0-9]+:[a-z0-9]+)*$/;

  const rules = {
    menuName: [
      { required: true, message: '菜單名稱不能為空' },
      {
        max: MENU_VALIDATION.NAME_MAX_LENGTH,
        message: `菜單名稱不能大於${MENU_VALIDATION.NAME_MAX_LENGTH}個字符`,
      },
    ],
    path:
      menuType === MenuTypeEnum.MENU
        ? [
            { required: true, message: '路由地址不能為空' },
            {
              max: MENU_VALIDATION.PATH_MAX_LENGTH,
              message: `路由地址不能大於${MENU_VALIDATION.PATH_MAX_LENGTH}個字符`,
            },
          ]
        : [],
    frameUrl:
      menuType === MenuTypeEnum.MENU && form.getFieldValue('frameFlag')
        ? [
            { required: true, message: '外鏈地址不能為空' },
            {
              max: MENU_VALIDATION.FRAME_URL_MAX_LENGTH,
              message: `外鏈地址不能大於${MENU_VALIDATION.FRAME_URL_MAX_LENGTH}個字符`,
            },
          ]
        : [],
    webPerms: [
      {
        pattern: PERMISSION_PATTERN,
        message: '權限格式錯誤！正確格式：module:resource:action（例如：system:menu:add）',
      },
    ],
    apiPerms: [
      {
        pattern: MULTI_PERMISSION_PATTERN,
        message: '權限格式錯誤！正確格式：module:resource:action，多個用逗號分隔（例如：system:menu:add,system:menu:update）',
      },
    ],
  };

  // ==================== Form Submission ====================

  /**
   * 處理表單提交
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();

      // 若無父級ID，默認設置為0
      const params = {
        ...values,
        parentId: values.parentId || MENU_CONSTANTS.TOP_PARENT_ID,
      };

      if (isEdit) {
        const updateForm: MenuUpdateForm = {
          ...params,
          menuId: initialData!.menuId!,
        };

        await menuApi.updateMenu(updateForm);
        message.success('更新成功');
        onSuccess();
      } else {
        const addForm: MenuAddForm = params;

        await menuApi.addMenu(addForm);
        message.success('添加成功');
        onSuccess();
      }
    } catch (error: any) {
      if (error.errorFields) {
        message.error('參數驗證錯誤，請仔細填寫表單數據！');
      } else {
        message.error(isEdit ? '更新失敗' : '添加失敗');
      }
    }
  };

  /**
   * 處理取消
   */
  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  /**
   * 處理菜單類型變更
   */
  const handleMenuTypeChange = (value: MenuTypeEnum) => {
    setMenuType(value);
  };

  // ==================== Render ====================

  return (
    <Drawer
      title={isEdit ? '編輯菜單' : '添加菜單'}
      open={visible}
      onClose={handleCancel}
      width={600}
      styles={{ body: { paddingBottom: 80 } }}
      destroyOnClose
      footer={
        <div style={{ textAlign: 'left' }}>
          <button
            type="button"
            onClick={handleCancel}
            style={{
              marginRight: 8,
              padding: '4px 15px',
              border: '1px solid #d9d9d9',
              borderRadius: '2px',
              background: '#fff',
              cursor: 'pointer',
            }}
          >
            取消
          </button>
          <button
            type="button"
            onClick={handleSubmit}
            style={{
              padding: '4px 15px',
              border: 'none',
              borderRadius: '2px',
              background: '#1890ff',
              color: '#fff',
              cursor: 'pointer',
            }}
          >
            提交
          </button>
        </div>
      }
    >
      <Form
        form={form}
        layout="vertical"
        initialValues={{
          menuType: MenuTypeEnum.CATALOG,
          permsType: PermsTypeEnum.SA_TOKEN,
          sort: 0,
          visibleFlag: true,
          cacheFlag: false,
          disabledFlag: false,
          frameFlag: false,
        }}
      >
        {/* 菜單類型 */}
        <Form.Item label="菜單類型" name="menuType">
          <Radio.Group buttonStyle="solid" onChange={e => handleMenuTypeChange(e.target.value)}>
            {Object.entries(MENU_TYPE_LABELS).map(([key, label]) => (
              <Radio.Button key={key} value={Number(key)}>
                {label}
              </Radio.Button>
            ))}
          </Radio.Group>
        </Form.Item>

        {/* 上級菜單/目錄 */}
        <Form.Item
          label={menuType === MenuTypeEnum.CATALOG ? '上級目錄' : '上級菜單'}
          name="parentId"
          help={
            menuType === MenuTypeEnum.CATALOG
              ? '目錄的上級只能是目錄或頂級'
              : menuType === MenuTypeEnum.MENU
                ? '菜單的上級只能是目錄'
                : '功能點的上級只能是菜單'
          }
        >
          <MenuTreeSelect
            menuType={menuType}
            currentMenuId={initialData?.menuId}
            placeholder={
              menuType === MenuTypeEnum.CATALOG
                ? '請選擇上級目錄'
                : menuType === MenuTypeEnum.MENU
                  ? '請選擇上級目錄'
                  : '請選擇上級菜單'
            }
          />
        </Form.Item>

        {/* 目錄、菜單共用欄位 */}
        {(menuType === MenuTypeEnum.CATALOG || menuType === MenuTypeEnum.MENU) && (
          <>
            <Form.Item label="菜單名稱" name="menuName" rules={rules.menuName}>
              <Input placeholder="請輸入菜單名稱" />
            </Form.Item>

            <Form.Item label="菜單圖標" name="icon" help="選擇 Ant Design 常用圖標">
              <IconSelect placeholder="請選擇菜單圖標" />
            </Form.Item>

            {menuType === MenuTypeEnum.MENU && (
              <>
                <Form.Item label="路由地址" name="path" rules={rules.path}>
                  <Input placeholder="請輸入路由地址" />
                </Form.Item>

                <Form.Item label="是否外鏈" name="frameFlag" valuePropName="checked">
                  <Switch checkedChildren="是外鏈" unCheckedChildren="不是外鏈" />
                </Form.Item>

                <Form.Item
                  noStyle
                  shouldUpdate={(prevValues, currentValues) =>
                    prevValues.frameFlag !== currentValues.frameFlag
                  }
                >
                  {({ getFieldValue }) =>
                    getFieldValue('frameFlag') ? (
                      <Form.Item label="外鏈地址" name="frameUrl" rules={rules.frameUrl}>
                        <Input placeholder="請輸入外鏈地址" />
                      </Form.Item>
                    ) : (
                      <Form.Item
                        label="組件地址"
                        name="component"
                        help="比如 商品列表：/business/erp/goods/goods-list.vue"
                      >
                        <Input placeholder="請輸入組件地址" />
                      </Form.Item>
                    )
                  }
                </Form.Item>

                <Form.Item label="是否緩存" name="cacheFlag" valuePropName="checked">
                  <Switch checkedChildren="開啟緩存" unCheckedChildren="不緩存" />
                </Form.Item>
              </>
            )}

            <Form.Item label="顯示狀態" name="visibleFlag" valuePropName="checked">
              <Switch checkedChildren="顯示" unCheckedChildren="不顯示" />
            </Form.Item>

            <Form.Item label="禁用狀態" name="disabledFlag" valuePropName="checked">
              <Switch checkedChildren="啟用" unCheckedChildren="禁用" />
            </Form.Item>
          </>
        )}

        {/* 功能點專屬欄位 */}
        {menuType === MenuTypeEnum.POINTS && (
          <>
            <Form.Item label="功能點名稱" name="menuName" rules={rules.menuName}>
              <Input placeholder="請輸入功能點名稱" />
            </Form.Item>

            <Form.Item
              label="功能點關聯菜單"
              name="contextMenuId"
              help="選擇該功能點所屬的菜單頁面（用於權限控制）"
            >
              <MenuTreeSelect
                menuType={MenuTypeEnum.MENU}
                currentMenuId={initialData?.menuId}
                placeholder="請選擇關聯菜單"
              />
            </Form.Item>

            <Form.Item label="功能點狀態" name="disabledFlag" valuePropName="checked">
              <Switch checkedChildren="啟用" unCheckedChildren="禁用" />
            </Form.Item>

            <Form.Item label="權限類型" name="permsType">
              <Radio.Group>
                {Object.entries(PERMS_TYPE_LABELS).map(([key, label]) => (
                  <Radio key={key} value={Number(key)}>
                    {label}
                  </Radio>
                ))}
              </Radio.Group>
            </Form.Item>

            <Form.Item
              label="前端權限"
              name="webPerms"
              rules={rules.webPerms}
              help="格式：module:resource:action（例如：system:menu:add）"
            >
              <Input placeholder="例如：system:menu:add" />
            </Form.Item>

            <Form.Item
              label="後端權限"
              name="apiPerms"
              rules={rules.apiPerms}
              help="格式：module:resource:action，多個用逗號分隔（例如：system:menu:add,system:menu:update）"
            >
              <Input placeholder="例如：system:menu:add,system:menu:update" />
            </Form.Item>
          </>
        )}

        {/* 排序 */}
        <Form.Item label="排序" name="sort" help="值越小越靠前">
          <InputNumber min={0} placeholder="請輸入排序" style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default MenuFormModal;
