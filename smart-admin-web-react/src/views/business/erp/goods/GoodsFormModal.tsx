/**
 * Goods Form Modal (Drawer)
 *
 * Corresponds to Vue's business/erp/goods/components/goods-form-modal.vue (153L)
 */
import React, { useEffect, useState } from 'react';
import { Drawer, Form, Input, InputNumber, Radio, Select, Button, Space, message } from 'antd';
import { goodsApi } from '@/api/business/erp/goods-api';
import type { GoodsVO } from '@/api/business/erp/goods-api';

interface Props {
  open: boolean;
  goods?: GoodsVO;
  onCancel: () => void;
  onSuccess: () => void;
}

const GoodsFormModal: React.FC<Props> = ({ open, goods, onCancel, onSuccess }) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const isEdit = !!goods;

  useEffect(() => {
    if (open) {
      if (goods) {
        form.setFieldsValue(goods);
      } else {
        form.resetFields();
        form.setFieldsValue({ goodsStatus: 1, shelvesFlag: true });
      }
    }
  }, [open, goods, form]);

  const handleSave = async () => {
    const values = await form.validateFields();
    setLoading(true);
    try {
      if (isEdit) {
        await goodsApi.update({ ...values, goodsId: goods!.goodsId });
      } else {
        await goodsApi.add(values);
      }
      message.success(isEdit ? '修改成功' : '添加成功');
      onSuccess();
    } finally {
      setLoading(false);
    }
  };

  return (
    <Drawer
      title={isEdit ? '编辑商品' : '添加商品'}
      open={open} onClose={onCancel} width={500}
      extra={<Space><Button onClick={onCancel}>取消</Button><Button type="primary" onClick={handleSave} loading={loading}>保存</Button></Space>}
    >
      <Form form={form} labelCol={{ span: 6 }}>
        <Form.Item label="商品名称" name="goodsName" rules={[{ required: true, message: '请输入商品名称' }]}>
          <Input placeholder="请输入商品名称" maxLength={100} />
        </Form.Item>
        <Form.Item label="状态" name="goodsStatus" rules={[{ required: true }]}>
          <Select options={[{ label: '预约中', value: 1 }, { label: '禁用', value: 2 }]} />
        </Form.Item>
        <Form.Item label="产地" name="place">
          <Input placeholder="请输入产地" maxLength={100} />
        </Form.Item>
        <Form.Item label="上架状态" name="shelvesFlag">
          <Radio.Group>
            <Radio value={true}>上架</Radio>
            <Radio value={false}>下架</Radio>
          </Radio.Group>
        </Form.Item>
        <Form.Item label="价格" name="price">
          <InputNumber min={0} precision={2} style={{ width: '100%' }} placeholder="请输入价格" />
        </Form.Item>
        <Form.Item label="备注" name="remark">
          <Input.TextArea rows={3} maxLength={500} placeholder="请输入备注" />
        </Form.Item>
      </Form>
    </Drawer>
  );
};

export default GoodsFormModal;
