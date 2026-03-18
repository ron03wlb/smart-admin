/**
 * Goods Form Drawer Component
 * 商品表單 Drawer 組件
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-11
 */

import React, { useEffect } from 'react';
import { Drawer, Form, Input, InputNumber, message, Radio, Select } from 'antd';
import { goodsApi } from '@/api/business/goodsApi';
import type {
  GoodsVO,
  GoodsAddForm,
  GoodsUpdateForm,
  GoodsFormData,
  GoodsStatusEnum,
} from '../types';
import { useModal } from '@/hooks/useModal';
import { GOODS_VALIDATION, GOODS_STATUS_LABELS } from '@/constants/business/goodsConst';

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
   * 表單驗證規則
   */
  const rules = {
    categoryId: [{ required: true, message: '請選擇商品分類' }],
    goodsName: [
      { required: true, message: '請輸入商品名稱' },
      {
        max: GOODS_VALIDATION.NAME_MAX_LENGTH,
        message: `商品名稱最多${GOODS_VALIDATION.NAME_MAX_LENGTH}個字符`,
      },
    ],
    goodsStatus: [{ required: true, message: '請選擇商品狀態' }],
    place: [{ required: true, message: '請選擇產地' }],
    price: [
      { required: true, message: '請輸入商品價格' },
      {
        type: 'number' as const,
        min: GOODS_VALIDATION.MIN_PRICE,
        max: GOODS_VALIDATION.MAX_PRICE,
        message: `價格範圍 ${GOODS_VALIDATION.MIN_PRICE} - ${GOODS_VALIDATION.MAX_PRICE}`,
      },
    ],
    remark: [
      {
        max: GOODS_VALIDATION.REMARK_MAX_LENGTH,
        message: `備註最多${GOODS_VALIDATION.REMARK_MAX_LENGTH}個字符`,
      },
    ],
  };

  /**
   * 表單提交
   */
  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      setLoading(true);

      if (isEdit && initialData) {
        // 編輯模式
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
        // 新增模式
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
        message.error(error.message || (isEdit ? '更新失敗' : '新增失敗'));
      }
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  /**
   * Drawer 關閉處理
   */
  const handleCancel = () => {
    form.resetFields();
    onCancel();
  };

  /**
   * 初始化表單數據（編輯模式）
   */
  useEffect(() => {
    if (visible && initialData) {
      // 處理產地字段（從逗號分隔字符串轉為數組）
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
      // 設置默認值
      form.setFieldsValue({
        shelvesFlag: true,
        goodsStatus: 1, // APPOINTMENT
      });
    }
  }, [visible, initialData, form]);

  return (
    <Drawer
      title={isEdit ? '編輯商品' : '新增商品'}
      open={visible}
      onClose={handleCancel}
      width={500}
      destroyOnClose
      footer={
        <div style={{ textAlign: 'right' }}>
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
            disabled={loading}
            style={{
              padding: '4px 15px',
              border: 'none',
              borderRadius: '2px',
              background: '#1890ff',
              color: '#fff',
              cursor: loading ? 'not-allowed' : 'pointer',
            }}
          >
            {loading ? '提交中...' : '提交'}
          </button>
        </div>
      }
    >
      <Form form={form} layout="vertical" preserve={false} style={{ marginTop: 16 }}>
        <Form.Item
          label="商品分類"
          name="categoryId"
          rules={rules.categoryId}
          tooltip="TODO: 應使用 CategoryTree 組件"
        >
          <InputNumber
            placeholder="請輸入商品分類ID（暫時）"
            style={{ width: '100%' }}
            min={1}
          />
        </Form.Item>

        <Form.Item label="商品名稱" name="goodsName" rules={rules.goodsName}>
          <Input
            placeholder="請輸入商品名稱"
            maxLength={GOODS_VALIDATION.NAME_MAX_LENGTH}
          />
        </Form.Item>

        <Form.Item label="商品狀態" name="goodsStatus" rules={rules.goodsStatus}>
          <Select placeholder="請選擇商品狀態">
            {Object.entries(GOODS_STATUS_LABELS).map(([value, label]) => (
              <Select.Option key={value} value={Number(value)}>
                {label}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item
          label="產地"
          name="place"
          rules={rules.place}
          tooltip="TODO: 應使用 DictSelect 組件"
        >
          <Select
            mode="tags"
            placeholder="請輸入產地（支援多選）"
            style={{ width: '100%' }}
          >
            <Select.Option value="中國">中國</Select.Option>
            <Select.Option value="日本">日本</Select.Option>
            <Select.Option value="美國">美國</Select.Option>
            <Select.Option value="歐洲">歐洲</Select.Option>
          </Select>
        </Form.Item>

        <Form.Item label="商品價格" name="price" rules={rules.price}>
          <InputNumber
            placeholder="請輸入商品價格"
            style={{ width: '100%' }}
            min={GOODS_VALIDATION.MIN_PRICE}
            max={GOODS_VALIDATION.MAX_PRICE}
            precision={2}
            prefix="¥"
          />
        </Form.Item>

        <Form.Item label="上架狀態" name="shelvesFlag">
          <Radio.Group>
            <Radio value={true}>上架</Radio>
            <Radio value={false}>下架</Radio>
          </Radio.Group>
        </Form.Item>

        <Form.Item label="備註" name="remark" rules={rules.remark}>
          <Input.TextArea
            placeholder="請輸入備註（可選）"
            maxLength={GOODS_VALIDATION.REMARK_MAX_LENGTH}
            showCount
            rows={4}
          />
        </Form.Item>
      </Form>
    </Drawer>
  );
}
