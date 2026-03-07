/**
 * Help Doc Catalog Form Modal
 *
 * Corresponds to Vue's support/help-doc/management/components/help-doc-catalog-form-modal.vue
 */
import React, { useEffect, useState } from 'react';
import { Modal, Form, Input, InputNumber, TreeSelect, message } from 'antd';
import { helpDocCatalogApi } from '@/api/support/help-doc-api';
import type { HelpDocCatalogVO } from '@/api/support/help-doc-api';

interface Props {
  open: boolean;
  catalog?: HelpDocCatalogVO;
  parentId: number;
  catalogList: HelpDocCatalogVO[];
  onCancel: () => void;
  onSuccess: () => void;
}

const buildTreeSelectData = (list: HelpDocCatalogVO[]): any[] => [
  { value: 0, title: '顶级目录', children: list.map((item) => ({ value: item.helpDocCatalogId, title: item.name, children: item.children ? buildTreeSelectData(item.children).flatMap((i) => i.children || []) : [] })) },
];

const HelpDocCatalogFormModal: React.FC<Props> = ({ open, catalog, parentId, catalogList, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!catalog;

  useEffect(() => {
    if (open) {
      if (catalog) {
        form.setFieldsValue({ name: catalog.name, parentId: catalog.parentId, sort: catalog.sort });
      } else {
        form.resetFields();
        form.setFieldsValue({ parentId, sort: 0 });
      }
    }
  }, [open, catalog, parentId, form]);

  const handleOk = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      if (isEdit) {
        await helpDocCatalogApi.update({ ...values, helpDocCatalogId: catalog!.helpDocCatalogId });
      } else {
        await helpDocCatalogApi.add(values);
      }
      message.success(isEdit ? '修改成功' : '添加成功');
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  const treeSelectData = buildTreeSelectData(catalogList);

  return (
    <Modal title={isEdit ? '编辑目录' : '添加目录'} open={open} onOk={handleOk} onCancel={onCancel} confirmLoading={loading} width={500} destroyOnClose>
      <Form form={form} labelCol={{ span: 6 }}>
        <Form.Item label="上级目录" name="parentId" rules={[{ required: true, message: '请选择上级目录' }]}>
          <TreeSelect treeData={treeSelectData} treeDefaultExpandAll placeholder="请选择上级目录" />
        </Form.Item>
        <Form.Item label="目录名称" name="name" rules={[{ required: true, message: '请输入目录名称' }]}>
          <Input placeholder="请输入目录名称" maxLength={50} />
        </Form.Item>
        <Form.Item label="排序" name="sort">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
      </Form>
    </Modal>
  );
};

export default HelpDocCatalogFormModal;
