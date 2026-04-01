/**
 * Goods Form Drawer Component
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import React, { useEffect } from 'react';
import { Drawer, Form, Input, InputNumber, message, Radio, Select, Button, Space } from 'antd';
import { goodsApi } from '@/api/business/goodsApi';
import type {
  GoodsVO,
  GoodsAddForm,
  GoodsUpdateForm,
  GoodsFormData,
} from '../types';
import { useModal } from '@/hooks/useModal';
import { GOODS_VALIDATION, GOODS_STATUS_LABELS } from '@/constants/business/goodsConst';
import CategoryTreeSelect from '@/components/common/CategoryTreeSelect';
import DictSelect from '@/components/common/DictSelect';
import { CategoryTypeEnum } from '@/views/business/category/types';
import { DICT_CODE_ENUM } from '@/constants/support/dictConst';

interface GoodsFormDrawerProps {
  visible: boolean;
  onCancel: () => void;
  onSuccess: () => void;
  initialData?: GoodsVO;
}

export default function GoodsFormDrawer({
  visible,
  onCancel,
  onSuccess,
  initialData,
}: GoodsFormDrawerProps) {
  const [form] = Form.useForm<GoodsFormData>();
  const [loading, setLoading] = React.useState(false);

  const { isEdit } = useModal<GoodsVO>({
    defaultFormData: initialData,
  });

  /**
   * Validation rules
   */
  const rules = {
    categoryId: [{ required: true, message: '请选择商品分类' }],
    goodsName: [
      { required: true, message: '请输入商品名称' },
      {
        max: GOODS_VALIDATION.NAME_MAX_LENGTH,
        message: `商品名称最多${GOODS_VALIDATION.NAME_MAX_LENGTH}个字符`,
      },
    ],
    goodsStatus: [{ required: true, message: '请选择商品状态' }],
    place: [{ required: true, message: '请选择产地' }],
    price: [
      { required: true, message: '请输入商品价格' },
      {
        type: 'number' as const,
        min: GOODS_VALIDATION.MIN_PRICE,
        max: GOODS_VALIDATION.MAX_PRICE,
        message: `价格范围 ${GOODS_VALIDATION.MIN_PRICE} - ${GOODS_VALIDATION.MAX_PRICE}`,
      },
    ],
    remark: [
      {
        max: GOODS_VALIDATION.REMARK_MAX_LENGTH,
        message: `备注最多${GOODS_VALIDATION.REMARK_MAX_LENGTH}个字符`,
      },
    ],
  };

  /**
   * Form submit
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (isEdit && initialData) {
        const updateForm: GoodsUpdateForm = {
          goodsId: initialData.goodsId,
          categoryId: values.categoryId!,
          goodsName: values.goodsName!,
          goodsStatus: values.goodsStatus!,
          place: values.place || [],
          price: values.price!,
          shelvesFlag: values.shelvesFlag !== undefined ? values.shelvesFlag : true,
          remark: values.remark,
        };

        const res = await goodsApi.updateGoods(updateForm);
        if (res.ok) {
          message.success('更新成功');
          onSuccess();
        }
      } else {
        const addForm: GoodsAddForm = {
          categoryId: values.categoryId!,
          goodsName: values.goodsName!,
          goodsStatus: values.goodsStatus!,
          place: values.place || [],
          price: values.price!,
          shelvesFlag: values.shelvesFlag !== undefined ? values.shelvesFlag : true,
          remark: values.remark,
        };

        const res = await goodsApi.addGoods(addForm);
        if (res.ok) {
          message.success('新增成功');
          onSuccess();
        }
      }
    } catch (error) {
      if (error instanceof Error) {
        message.error(error.message || (isEdit ? '更新失败' : '新增失败'));
      }
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  /**
   * Initialize form data (edit mode)
   */
  useEffect(() => {
    if (visible && initialData) {
      const placeArray = initialData.place ? initialData.place.split(',') : [];

      form.setFieldsValue({
        categoryId: initialData.categoryId,
        goodsName: initialData.goodsName,
        goodsStatus: initialData.goodsStatus,
        place: placeArray,
        price: initialData.price,
        shelvesFlag: initialData.shelvesFlag,
        remark: initialData.remark,
      });
    } else if (visible && !initialData) {
      form.resetFields();
      form.setFieldsValue({
        shelvesFlag: true,
        goodsStatus: 1,
      });
    }
  }, [visible, initialData, form]);

  return (
    <Drawer
      title={isEdit ? '编辑商品' : '新增商品'}
      open={visible}
      onClose={handleCancel}
      width={500}
      destroyOnClose
      extra={
        <Space>
          <Button onClick={handleCancel}>取消</Button>
          <Button type="primary" onClick={handleSubmit} loading={loading}>
            提交
          </Button>
        </Space>
      }
    >
      <Form form={form} layout="vertical" preserve={false} style={{ marginTop: 16 }}>
        <Form.Item label="商品分类" name="categoryId" rules={rules.categoryId}>
          <CategoryTreeSelect
            categoryType={CategoryTypeEnum.GOODS}
            placeholder="请选择商品分类"
          />
        </Form.Item>

        <Form.Item label="商品名称" name="goodsName" rules={rules.goodsName}>
          <Input placeholder="请输入商品名称" maxLength={GOODS_VALIDATION.NAME_MAX_LENGTH} />
        </Form.Item>

        <Form.Item label="商品状态" name="goodsStatus" rules={rules.goodsStatus}>
          <Select placeholder="请选择商品状态">
            {Object.entries(GOODS_STATUS_LABELS).map(([value, label]) => (
              <Select.Option key={value} value={Number(value)}>
                {label}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item label="产地" name="place" rules={rules.place}>
          <DictSelect<string[]>
            dictCode={DICT_CODE_ENUM.GOODS_PLACE}
            mode="tags"
            placeholder="请选择产地（支持多选）"
            style={{ width: '100%' }}
          />
        </Form.Item>

        <Form.Item label="商品价格" name="price" rules={rules.price}>
          <InputNumber
            placeholder="请输入商品价格"
            style={{ width: '100%' }}
            min={GOODS_VALIDATION.MIN_PRICE}
            max={GOODS_VALIDATION.MAX_PRICE}
            precision={2}
            prefix="&yen;"
          />
        </Form.Item>

        <Form.Item label="上架状态" name="shelvesFlag">
          <Radio.Group>
            <Radio value={true}>上架</Radio>
            <Radio value={false}>下架</Radio>
          </Radio.Group>
        </Form.Item>

        <Form.Item label="备注" name="remark" rules={rules.remark}>
          <Input.TextArea
            placeholder="请输入备注（可选）"
            maxLength={GOODS_VALIDATION.REMARK_MAX_LENGTH}
            showCount
            rows={4}
          />
        </Form.Item>
      </Form>
    </Drawer>
  );
}
