/**
 * Code Generator Insert Update Form - 增加/修改表單（簡化版）
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/code-generator/components/form/code-generator-table-config-form-insert-and-update.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-15
 */

import { useState, useImperativeHandle, forwardRef } from 'react';
import { Form, Radio, InputNumber, Alert, Row, Col } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';

export interface InsertUpdateFormRef {
  setData: (columns: any[], config: any) => void;
  getFormData: () => any;
  validateForm: () => Promise<boolean>;
}

const InsertUpdateForm = forwardRef<InsertUpdateFormRef>((_props, ref) => {
  const [form] = Form.useForm();
  const [isSupportInsertAndUpdate, setIsSupportInsertAndUpdate] = useState(true);
  const [_pageType, setPageType] = useState('modal');
  const [countPerLine, setCountPerLine] = useState(1);

  /**
   * 設置數據
   */
  const setData = (_columns: any[], config: any) => {
    const insertAndUpdate = config?.insertAndUpdate || {};

    const formData = {
      isSupportInsertAndUpdate: insertAndUpdate.isSupportInsertAndUpdate !== false,
      pageType: insertAndUpdate.pageType || 'modal',
      width: insertAndUpdate.width || '800',
      countPerLine: insertAndUpdate.countPerLine || 1,
    };

    form.setFieldsValue(formData);
    setIsSupportInsertAndUpdate(formData.isSupportInsertAndUpdate);
    setPageType(formData.pageType);
    setCountPerLine(formData.countPerLine);
  };

  /**
   * 獲取表單數據
   */
  const getFormData = () => {
    return form.getFieldsValue();
  };

  /**
   * 驗證表單
   */
  const validateForm = (): Promise<boolean> => {
    return new Promise((resolve) => {
      form
        .validateFields()
        .then(() => {
          resolve(true);
        })
        .catch(() => {
          resolve(false);
        });
    });
  };

  // 暴露方法給父組件
  useImperativeHandle(ref, () => ({
    setData,
    getFormData,
    validateForm,
  }));

  /**
   * 計算每行span
   */
  const spanPerLine = Math.floor(20 / countPerLine);

  return (
    <div>
      <Alert
        closable
        message="完整增加/修改配置功能（字段選擇、前端組件配置、required驗證等）將在 Phase 3 實現"
        type="info"
        showIcon
        icon={<InfoCircleOutlined />}
        style={{ marginBottom: 16 }}
      />

      <Row gutter={16}>
        <Col span={10}>
          <Form
            form={form}
            labelCol={{ span: 8 }}
            wrapperCol={{ span: 16 }}
            onValuesChange={(changedValues) => {
              if ('isSupportInsertAndUpdate' in changedValues) {
                setIsSupportInsertAndUpdate(changedValues.isSupportInsertAndUpdate);
              }
              if ('pageType' in changedValues) {
                setPageType(changedValues.pageType);
              }
              if ('countPerLine' in changedValues) {
                setCountPerLine(changedValues.countPerLine);
              }
            }}
          >
            <Form.Item
              label="是否支持"
              name="isSupportInsertAndUpdate"
              initialValue={true}
            >
              <Radio.Group buttonStyle="solid">
                <Radio.Button value={true}>支持</Radio.Button>
                <Radio.Button value={false}>不支持添加、修改</Radio.Button>
              </Radio.Group>
            </Form.Item>

            {isSupportInsertAndUpdate && (
              <>
                <Form.Item
                  label="頁面方式"
                  name="pageType"
                  initialValue="modal"
                  rules={[{ required: true, message: '請選擇頁面方式' }]}
                >
                  <Radio.Group buttonStyle="solid">
                    <Radio.Button value="modal">Modal</Radio.Button>
                    <Radio.Button value="drawer">Drawer</Radio.Button>
                  </Radio.Group>
                </Form.Item>

                <Form.Item
                  label="頁面寬度"
                  name="width"
                  initialValue="800"
                  rules={[{ required: true, message: '請輸入頁面寬度' }]}
                >
                  <InputNumber style={{ width: '100%' }} placeholder="Modal或Drawer的width屬性" />
                </Form.Item>

                <Form.Item
                  label="每行數量"
                  name="countPerLine"
                  initialValue={1}
                  rules={[{ required: true, message: '請輸入每行數量' }]}
                >
                  <InputNumber style={{ width: '100%' }} min={1} max={4} />
                </Form.Item>
              </>
            )}
          </Form>
        </Col>

        {isSupportInsertAndUpdate && (
          <Col span={14}>
            <div style={{ padding: 16, border: '1px dashed #d9d9d9', borderRadius: 4 }}>
              <div style={{ marginBottom: 8, fontWeight: 500 }}>表單預覽</div>
              <Row gutter={16}>
                {Array.from({ length: countPerLine }).map((_, index) => (
                  <Col key={index} span={spanPerLine}>
                    <div
                      style={{
                        padding: 16,
                        marginBottom: 8,
                        backgroundColor: '#f5f5f5',
                        borderRadius: 4,
                        textAlign: 'center',
                      }}
                    >
                      字段
                    </div>
                  </Col>
                ))}
              </Row>
              <Row gutter={16}>
                {Array.from({ length: countPerLine }).map((_, index) => (
                  <Col key={index} span={spanPerLine}>
                    <div
                      style={{
                        padding: 16,
                        marginBottom: 8,
                        backgroundColor: '#f5f5f5',
                        borderRadius: 4,
                        textAlign: 'center',
                      }}
                    >
                      字段
                    </div>
                  </Col>
                ))}
              </Row>
            </div>
          </Col>
        )}
      </Row>
    </div>
  );
});

InsertUpdateForm.displayName = 'InsertUpdateForm';

export default InsertUpdateForm;
