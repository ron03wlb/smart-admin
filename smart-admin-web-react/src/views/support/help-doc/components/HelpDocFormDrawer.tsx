/**
 * Help Doc Form Drawer
 * 幫助文檔表單抽屜
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { useState, forwardRef, useImperativeHandle } from 'react';
import { Drawer, Form, Input, InputNumber, Radio, Space, Button, message } from 'antd';
import { helpDocApi } from '@/api/support/helpDocApi';
import type { HelpDocFormData, FileInfo } from '../types';
import HelpDocCatalogTreeSelect from './HelpDocCatalogTreeSelect';

const { TextArea } = Input;

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
    const [contentHtml, setContentHtml] = useState('');
    const [defaultFileList, setDefaultFileList] = useState<FileInfo[]>([]);

    useImperativeHandle(ref, () => ({
      showDrawer: async (helpDocId?: number) => {
        form.resetFields();
        setDefaultFileList([]);
        setContentHtml('');
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

        if (data.attachment && data.attachment.length > 0) {
          setDefaultFileList(data.attachment);
        }

        setContentHtml(data.contentHtml);

        const relationIdList = data.relationList ? data.relationList.map((e) => e.relationId) : [];
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

        // TODO: 從富文本編輯器獲取內容
        // values.contentHtml = richTextEditor.getHtml();
        // values.contentText = richTextEditor.getText();
        values.contentHtml = contentHtml;
        values.contentText = contentHtml; // Temporary: should extract text from HTML

        // 處理關聯關係
        if (relateHomeFlag) {
          values.relationList = [
            {
              relationName: '首頁',
              relationId: 0,
            },
          ];
        }
        // TODO: 如果不是首頁顯示，需要從 MenuTreeSelect 獲取選中的菜單列表
        // else {
        //   const relationList = menuTreeSelect.current.getMenuListByIdList(values.relationIdList);
        //   values.relationList = relationList.map((e) => ({ relationId: e.menuId, relationName: e.menuName }));
        // }

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

    // const changeAttachment = (fileList: FileInfo[]) => {
    //   setDefaultFileList(fileList);
    //   form.setFieldsValue({ attachment: fileList });
    // };

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
        <Form
          form={form}
          labelCol={{ span: 3 }}
          wrapperCol={{ span: 20 }}
        >
          <Form.Item name="helpDocId" hidden>
            <Input />
          </Form.Item>

          <Form.Item
            label="標題"
            name="title"
            rules={[{ required: true, message: '請輸入標題' }]}
          >
            <Input placeholder="請輸入標題" />
          </Form.Item>

          <Form.Item
            label="目錄"
            name="helpDocCatalogId"
            rules={[{ required: true, message: '請選擇目錄' }]}
          >
            <HelpDocCatalogTreeSelect />
          </Form.Item>

          <Form.Item
            label="作者"
            name="author"
            rules={[{ required: true, message: '請輸入作者' }]}
          >
            <Input placeholder="請輸入作者" />
          </Form.Item>

          <Form.Item
            label="排序"
            name="sort"
            rules={[{ required: true, message: '請輸入排序' }]}
          >
            <InputNumber placeholder="值越小越靠前" style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item label="是否首頁顯示">
            <Radio.Group
              value={relateHomeFlag}
              onChange={(e) => setRelateHomeFlag(e.target.value)}
              buttonStyle="solid"
            >
              <Radio.Button value={true}>首頁顯示</Radio.Button>
              <Radio.Button value={false}>首頁不用顯示</Radio.Button>
            </Radio.Group>
          </Form.Item>

          {!relateHomeFlag && (
            <Form.Item label="關聯菜單" name="relationIdList">
              {/* TODO: 集成 MenuTreeSelect 組件 */}
              <Input placeholder="TODO: 需要集成 MenuTreeSelect 組件" disabled />
            </Form.Item>
          )}

          <Form.Item
            label="內容"
            name="contentHtml"
            rules={[{ required: true, message: '請輸入內容' }]}
          >
            {/* TODO: 集成富文本編輯器（ReactQuill, Draft.js, 或其他） */}
            <TextArea
              value={contentHtml}
              onChange={(e) => setContentHtml(e.target.value)}
              rows={10}
              placeholder="TODO: 需要集成富文本編輯器組件（建議使用 ReactQuill 或 Draft.js）"
            />
          </Form.Item>

          <Form.Item label="附件" name="attachment">
            {/* TODO: 集成文件上傳組件 */}
            <div>
              <p>TODO: 需要集成文件上傳組件</p>
              <p>已上傳附件：{defaultFileList.length} 個</p>
            </div>
          </Form.Item>
        </Form>
      </Drawer>
    );
  }
);

HelpDocFormDrawer.displayName = 'HelpDocFormDrawer';

export default HelpDocFormDrawer;
