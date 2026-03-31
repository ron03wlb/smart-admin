/**
 * Help Doc Form Drawer
 * 幫助文檔表單抽屜
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { useState, useRef, forwardRef, useImperativeHandle } from 'react';
import { Drawer, Form, Input, InputNumber, Radio, Space, Button, message } from 'antd';
import { helpDocApi } from '@/api/support/helpDocApi';
import type { HelpDocFormData } from '../types';
import HelpDocCatalogTreeSelect from './HelpDocCatalogTreeSelect';
import RichTextEditor from '@/components/RichTextEditor/RichTextEditor';
import MenuTreeSelect from '@/components/system/menu-tree-select/MenuTreeSelect';
import type { MenuTreeSelectRef } from '@/components/system/menu-tree-select/MenuTreeSelect';
import FileUpload from '@/components/support/file-upload/FileUpload';

export interface HelpDocFormDrawerProps {
  onReloadList: () => void;
}

export interface HelpDocFormDrawerRef {
  showDrawer: (helpDocId?: number) => void;
}

const HelpDocFormDrawer = forwardRef<HelpDocFormDrawerRef, HelpDocFormDrawerProps>(
  ({ onReloadList }, ref) => {
    const [form] = Form.useForm<HelpDocFormData>();
    const [visible, setVisible] = useState(false);
    const [loading, setLoading] = useState(false);
    const [relateHomeFlag, setRelateHomeFlag] = useState(false);
    const menuTreeSelectRef = useRef<MenuTreeSelectRef>(null);

    useImperativeHandle(ref, () => ({
      showDrawer: async (helpDocId?: number) => {
        form.resetFields();
        setRelateHomeFlag(false);

        if (helpDocId) {
          await getDetail(helpDocId);
        }

        setVisible(true);
      },
    }));

    const getDetail = async (helpDocId: number) => {
      try {
        setLoading(true);
        const result = await helpDocApi.getDetail(helpDocId);
        const data = result.data;

        const relationIdList = data.relationList ? data.relationList.map(e => e.relationId) : [];
        if (relationIdList.length === 1 && relationIdList[0] === 0) {
          setRelateHomeFlag(true);
        } else {
          setRelateHomeFlag(false);
        }

        form.setFieldsValue({
          helpDocId: data.helpDocId,
          helpDocCatalogId: data.helpDocCatalogId,
          title: data.title,
          author: data.author,
          sort: data.sort,
          attachment: data.attachment || [],
          relationList: data.relationList || [],
          contentHtml: data.contentHtml,
          contentText: data.contentText,
        });
      } catch (error) {
        console.error('Failed to get help doc detail:', error);
      } finally {
        setLoading(false);
      }
    };

    const handleSubmit = async () => {
      try {
        await form.validateFields();
        const values = form.getFieldsValue();

        // RichTextEditor content is synced via Form value/onChange
        // Extract plain text from HTML for contentText field
        const parser = new DOMParser();
        const doc = parser.parseFromString(values.contentHtml || '', 'text/html');
        values.contentText = doc.body.textContent || '';

        // Handle relation list (home page or menu association)
        if (relateHomeFlag) {
          values.relationList = [
            {
              relationName: '首頁',
              relationId: 0,
            },
          ];
        } else if (menuTreeSelectRef.current) {
          const relationIdList = form.getFieldValue('relationIdList') || [];
          const menuList = menuTreeSelectRef.current.getMenuListByIdList(relationIdList);
          values.relationList = menuList.map((e) => ({ relationId: Number(e.menuId), relationName: e.menuName }));
        }

        setLoading(true);
        if (values.helpDocId) {
          await helpDocApi.update(values);
        } else {
          await helpDocApi.add(values);
        }

        message.success('保存成功');
        setVisible(false);
        form.resetFields();
        onReloadList();
      } catch (error: any) {
        if (error.errorFields) {
          message.error('參數驗證錯誤，請仔細填寫表單數據!');
        } else {
          console.error('Failed to save help doc:', error);
        }
      } finally {
        setLoading(false);
      }
    };

    const handleClose = () => {
      setVisible(false);
      form.resetFields();
    };

    return (
      <Drawer
        open={visible}
        title={form.getFieldValue('helpDocId') ? '編輯系統手冊' : '新建系統手冊'}
        width={1000}
        onClose={handleClose}
        footer={
          <div style={{ textAlign: 'right' }}>
            <Space>
              <Button onClick={handleClose}>取消</Button>
              <Button type="primary" loading={loading} onClick={handleSubmit}>
                保存
              </Button>
            </Space>
          </div>
        }
        destroyOnClose
      >
        <Form form={form} labelCol={{ span: 3 }} wrapperCol={{ span: 20 }}>
          <Form.Item name="helpDocId" hidden>
            <Input />
          </Form.Item>

          <Form.Item label="標題" name="title" rules={[{ required: true, message: '請輸入標題' }]}>
            <Input placeholder="請輸入標題" />
          </Form.Item>

          <Form.Item
            label="目錄"
            name="helpDocCatalogId"
            rules={[{ required: true, message: '請選擇目錄' }]}
          >
            <HelpDocCatalogTreeSelect />
          </Form.Item>

          <Form.Item label="作者" name="author" rules={[{ required: true, message: '請輸入作者' }]}>
            <Input placeholder="請輸入作者" />
          </Form.Item>

          <Form.Item label="排序" name="sort" rules={[{ required: true, message: '請輸入排序' }]}>
            <InputNumber placeholder="值越小越靠前" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item label="是否首頁顯示">
            <Radio.Group
              value={relateHomeFlag}
              onChange={e => setRelateHomeFlag(e.target.value)}
              buttonStyle="solid"
            >
              <Radio.Button value={true}>首頁顯示</Radio.Button>
              <Radio.Button value={false}>首頁不用顯示</Radio.Button>
            </Radio.Group>
          </Form.Item>

          {!relateHomeFlag && (
            <Form.Item label="關聯菜單" name="relationIdList">
              <MenuTreeSelect ref={menuTreeSelectRef} />
            </Form.Item>
          )}

          <Form.Item
            label="內容"
            name="contentHtml"
            rules={[{ required: true, message: '請輸入內容' }]}
          >
            <RichTextEditor placeholder="請輸入幫助文檔內容" />
          </Form.Item>

          <Form.Item label="附件" name="attachment">
            <FileUpload
              maxCount={10}
              maxSize={10}
              listType="text"
            />
          </Form.Item>
        </Form>
      </Drawer>
    );
  }
);

HelpDocFormDrawer.displayName = 'HelpDocFormDrawer';

export default HelpDocFormDrawer;
