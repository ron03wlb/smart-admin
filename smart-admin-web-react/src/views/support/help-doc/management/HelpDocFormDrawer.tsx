/**
 * Help Doc Form Drawer
 *
 * Corresponds to Vue's support/help-doc/management/components/help-doc-form-drawer.vue
 * Simplified: uses TextArea instead of rich text editor (WangEditor)
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Drawer, Form, Input, InputNumber, TreeSelect, Button, Space, message } from 'antd';
import RichTextEditor from '@/components/RichTextEditor/RichTextEditor';
import { helpDocApi, helpDocCatalogApi } from '@/api/support/help-doc-api';
import type { HelpDocVO, HelpDocCatalogVO } from '@/api/support/help-doc-api';

interface Props {
  open: boolean;
  helpDoc?: HelpDocVO;
  onClose: () => void;
  onSuccess: () => void;
}

const buildTreeSelectData = (list: HelpDocCatalogVO[]): any[] =>
  list.map((item) => ({
    value: item.helpDocCatalogId,
    title: item.name,
    children: item.children ? buildTreeSelectData(item.children) : [],
  }));

const HelpDocFormDrawer: React.FC<Props> = ({ open, helpDoc, onClose, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [catalogTree, setCatalogTree] = useState<any[]>([]);
  const isEdit = !!helpDoc;

  const loadCatalogs = useCallback(async () => {
    const res = await helpDocCatalogApi.getAll();
    if (res.code === 1 && res.data) {
      setCatalogTree(buildTreeSelectData(res.data));
    }
  }, []);

  useEffect(() => {
    if (open) {
      loadCatalogs();
      if (helpDoc) {
        // Load full detail for edit
        helpDocApi.getDetail(helpDoc.helpDocId).then((res) => {
          if (res.code === 1 && res.data) {
            form.setFieldsValue(res.data);
          }
        });
      } else {
        form.resetFields();
        form.setFieldsValue({ sort: 0 });
      }
    }
  }, [open, helpDoc, form, loadCatalogs]);

  const handleSave = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      const payload = {
        ...values,
        contentText: values.contentHtml?.replace(/<[^>]+>/g, '') || '',
      };
      if (isEdit) {
        await helpDocApi.update({ ...payload, helpDocId: helpDoc!.helpDocId });
      } else {
        await helpDocApi.add(payload);
      }
      message.success(isEdit ? '修改成功' : '添加成功');
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Drawer
      title={isEdit ? '编辑文档' : '新建文档'}
      open={open}
      onClose={onClose}
      width={800}
      extra={
        <Space>
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" onClick={handleSave} loading={loading}>保存</Button>
        </Space>
      }
    >
      <Form form={form} labelCol={{ span: 4 }}>
        <Form.Item label="标题" name="title" rules={[{ required: true, message: '请输入标题' }]}>
          <Input placeholder="请输入文档标题" maxLength={200} />
        </Form.Item>
        <Form.Item label="所属目录" name="helpDocCatalogId" rules={[{ required: true, message: '请选择所属目录' }]}>
          <TreeSelect treeData={catalogTree} treeDefaultExpandAll placeholder="请选择目录" />
        </Form.Item>
        <Form.Item label="作者" name="author" rules={[{ required: true, message: '请输入作者' }]}>
          <Input placeholder="请输入作者" maxLength={50} />
        </Form.Item>
        <Form.Item label="排序" name="sort">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label="内容" name="contentHtml" rules={[{ required: true, message: '请输入文档内容' }]}>
          <RichTextEditor placeholder="请输入文档内容" />
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default HelpDocFormDrawer;
