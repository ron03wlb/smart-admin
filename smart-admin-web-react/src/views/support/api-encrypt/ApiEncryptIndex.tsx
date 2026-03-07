/**
 * API Encrypt/Decrypt Demo
 *
 * Corresponds to Vue's support/api-encrypt/api-encrypt-index.vue (238L)
 * Demonstrates SM4/AES request/response encryption.
 */
import React, { useState } from 'react';
import { Card, Form, Input, InputNumber, Button, Typography, Alert } from 'antd';
import { apiEncryptApi } from '@/api/support/api-encrypt-api';

const { Text } = Typography;

interface TestResult {
  data?: any;
  encryptData?: string;
}

const ApiEncryptIndex: React.FC = () => {
  const [form1] = Form.useForm();
  const [form2] = Form.useForm();
  const [form3] = Form.useForm();
  const [result1, setResult1] = useState<TestResult>({});
  const [result2, setResult2] = useState<TestResult>({});
  const [result3, setResult3] = useState<TestResult>({});
  const [result4, setResult4] = useState<TestResult>({});

  const handleTest = async (type: 1 | 2 | 3 | 4) => {
    const formMap = { 1: form1, 2: form2, 3: form3, 4: form1 };
    const values = await formMap[type].validateFields();
    const apiMap = {
      1: () => apiEncryptApi.testRequestEncrypt(values),
      2: () => apiEncryptApi.testResponseEncrypt(values),
      3: () => apiEncryptApi.testDecryptAndEncrypt(values),
      4: () => apiEncryptApi.testArray([values]),
    };
    const res = await apiMap[type]();
    if (res.code === 1) {
      const resultMap = { 1: setResult1, 2: setResult2, 3: setResult3, 4: setResult4 };
      resultMap[type](res.data || {});
    }
  };

  const renderForm = (form: any, idx: 1 | 2 | 3 | 4, title: string, result: TestResult) => (
    <Card title={title} style={{ marginBottom: 16 }} size="small">
      <Form form={form} layout="inline" initialValues={{ name: '卓大', age: 18 }}>
        <Form.Item label="姓名" name="name" rules={[{ required: true }]}>
          <Input style={{ width: 120 }} />
        </Form.Item>
        <Form.Item label="年龄" name="age" rules={[{ required: true }]}>
          <InputNumber style={{ width: 80 }} />
        </Form.Item>
        <Form.Item>
          <Button type="primary" onClick={() => handleTest(idx)}>测试</Button>
        </Form.Item>
      </Form>
      {(result.data || result.encryptData) && (
        <div style={{ marginTop: 12 }}>
          {result.data && <div><Text strong>解密数据：</Text><Text code>{JSON.stringify(result.data)}</Text></div>}
          {result.encryptData && <div style={{ marginTop: 4 }}><Text strong>加密数据：</Text><Text code style={{ wordBreak: 'break-all' }}>{result.encryptData}</Text></div>}
        </div>
      )}
    </Card>
  );

  return (
    <div>
      <Alert type="info" showIcon message="接口加解密演示：测试 SM4/AES 对 Request/Response 的加解密能力" style={{ marginBottom: 16 }} />
      {renderForm(form1, 1, '1. 请求加密（Request Encrypt）', result1)}
      {renderForm(form2, 2, '2. 响应加密（Response Encrypt）', result2)}
      {renderForm(form3, 3, '3. 双向加解密（Decrypt & Encrypt）', result3)}
      {renderForm(form1, 4, '4. 数组加密（Array Encrypt）', result4)}
    </div>
  );
};

export default ApiEncryptIndex;
