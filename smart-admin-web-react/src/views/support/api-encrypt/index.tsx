/**
 * API Encrypt Index
 * 接口加密測試頁面
 *
 * 參考：Vue 版本 smart-admin-web/src/views/support/api-encrypt/api-encrypt-index.vue
 *
 * @Author: SmartAdmin React Team
 * @Date: 2026-03-14
 */

import { useState } from 'react';
import { Alert, Card, Form, Input, InputNumber, Button, Row, Col, Space } from 'antd';
import { message } from 'antd';
import { apiEncryptApi, type EncryptTestForm } from '@/api/support/apiEncryptApi';

const ApiEncryptPage: React.FC = () => {
  // ==================== 一、請求加密 ====================
  const [requestEncryptForm] = Form.useForm();
  const [requestEncryptFormStr, setRequestEncryptFormStr] = useState('');
  const [requestEncryptResponse, setRequestEncryptResponse] = useState('');

  const testRequestEncrypt = async () => {
    try {
      const values = await requestEncryptForm.validateFields();
      setRequestEncryptFormStr(JSON.stringify(values));
      const result = await apiEncryptApi.testRequestEncrypt(values);
      setRequestEncryptResponse(JSON.stringify(result.data));
      message.success('請求加密測試成功');
    } catch (error) {
      console.error('請求加密測試失敗:', error);
    }
  };

  // ==================== 二、返回加密 ====================
  const [responseEncryptForm] = Form.useForm();
  const [responseEncryptFormStr, setResponseEncryptFormStr] = useState('');
  const [responseEncryptStr, setResponseEncryptStr] = useState('');
  const [responseStr, setResponseStr] = useState('');

  const testResponseEncrypt = async () => {
    try {
      const values = await responseEncryptForm.validateFields();
      setResponseEncryptFormStr(JSON.stringify(values));
      const result: any = await apiEncryptApi.testResponseEncrypt(values);
      setResponseEncryptStr(result.encryptData || '');
      setResponseStr(JSON.stringify(result.data));
      message.success('返回加密測試成功');
    } catch (error) {
      console.error('返回加密測試失敗:', error);
    }
  };

  // ==================== 三、請求和返回都加密 ====================
  const [bothForm] = Form.useForm();
  const [formStr, setFormStr] = useState('');
  const [responseEncrypt, setResponseEncrypt] = useState('');
  const [responseDecryptStr, setResponseDecryptStr] = useState('');

  const testBoth = async () => {
    try {
      const values = await bothForm.validateFields();
      setFormStr(JSON.stringify(values));
      const result: any = await apiEncryptApi.testDecryptAndEncrypt(values);
      setResponseEncrypt(result.encryptData || '');
      setResponseDecryptStr(JSON.stringify(result.data));
      message.success('請求和返回加密測試成功');
    } catch (error) {
      console.error('請求和返回加密測試失敗:', error);
    }
  };

  // ==================== 四、測試數組 ====================
  const [arrayFormStr, setArrayFormStr] = useState('');
  const [arrayFormResponseEncrypt, setArrayFormResponseEncrypt] = useState('');
  const [arrayFormResponseDecryptStr, setArrayFormResponseDecryptStr] = useState('');

  const testArray = async () => {
    try {
      const arrayData = [
        { age: 1, name: '卓1' },
        { age: 2, name: '卓2' },
        { age: 3, name: '卓3' },
      ];
      setArrayFormStr(JSON.stringify(arrayData));
      const result: any = await apiEncryptApi.testArray(arrayData);
      setArrayFormResponseEncrypt(result.encryptData || '');
      setArrayFormResponseDecryptStr(JSON.stringify(result.data));
      message.success('數組加解密測試成功');
    } catch (error) {
      console.error('數組加解密測試失敗:', error);
    }
  };

  return (
    <div style={{ padding: '20px' }}>
      {/* 說明 Alert */}
      <Alert
        type="info"
        closable
        message={<h4>接口加解密：</h4>}
        description={
          <pre style={{ whiteSpace: 'pre-wrap' }}>
            {`簡介：接口加解密分為： 前端請求參數加解密 和 後端返回結果加解密。

- 支持國密SM、AES加密算法。
- 前端請看：/lib/encrypt.js、/lib/axios.js  /api/support/api-encrypt/api-encrypt-api.js 等文件
- 後端請看：@ApiEncrypt 和 @ApiDecrypt 注解
- demo請看：前端：/views/support/api-encrypt 目錄`}
          </pre>
        }
      />

      <br />

      <Alert
        type="error"
        message="當前加密算法為：SM4，若想改為 AES，前端請修改 'lib/encrypt.js'文件中的EncryptObject"
      />

      <br />

      {/* 一、請求加密 Demo */}
      <Card title="一、請求加密 Demo">
        <Form form={requestEncryptForm} initialValues={{ name: '卓大', age: 100 }}>
          <Row gutter={16}>
            <Col span={6}>
              <Form.Item label="姓名" name="name">
                <Input placeholder="姓名" />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="年齡" name="age">
                <InputNumber placeholder="年齡" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item>
                <Button type="primary" onClick={testRequestEncrypt}>
                  測試：請求加密
                </Button>
              </Form.Item>
            </Col>
          </Row>
        </Form>
        {requestEncryptFormStr && <div>請求參數：{requestEncryptFormStr}</div>}
        {requestEncryptResponse && <div>返回結果（不加密）：{requestEncryptResponse}</div>}
      </Card>

      <br />

      {/* 二、返回加密 Demo */}
      <Card title="二、返回加密 Demo">
        <Form form={responseEncryptForm} initialValues={{ name: '卓大', age: 100 }}>
          <Row gutter={16}>
            <Col span={6}>
              <Form.Item label="姓名" name="name">
                <Input placeholder="姓名" />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="年齡" name="age">
                <InputNumber placeholder="年齡" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item>
                <Button type="primary" onClick={testResponseEncrypt}>
                  測試：返回加密
                </Button>
              </Form.Item>
            </Col>
          </Row>
        </Form>
        {responseEncryptFormStr && <div>請求參數： {responseEncryptFormStr}</div>}
        {responseEncryptStr && <div>返回結果：{responseEncryptStr}</div>}
        {responseStr && <div>返回結果 解密：{responseStr}</div>}
      </Card>

      <br />

      {/* 三、請求和返回都加密 Demo */}
      <Card title="三、請求和返回都加密 Demo">
        <Form form={bothForm} initialValues={{ name: '卓大', age: 100 }}>
          <Row gutter={16}>
            <Col span={6}>
              <Form.Item label="姓名" name="name">
                <Input placeholder="姓名" />
              </Form.Item>
            </Col>
            <Col span={6}>
              <Form.Item label="年齡" name="age">
                <InputNumber placeholder="年齡" style={{ width: '100%' }} />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item>
                <Button type="primary" onClick={testBoth}>
                  測試：請求和返回都加密
                </Button>
              </Form.Item>
            </Col>
          </Row>
        </Form>
        {formStr && <div>請求參數： {formStr}</div>}
        {responseEncrypt && <div>返回結果：{responseEncrypt}</div>}
        {responseDecryptStr && <div>返回結果 解密：{responseDecryptStr}</div>}
      </Card>

      <br />

      {/* 四、測試數組 Demo */}
      <Card title="四、測試數組 Demo">
        <Space>
          <Button type="primary" onClick={testArray}>
            測試：數組加解密
          </Button>
        </Space>
        <br />
        <br />
        {arrayFormStr && <div>請求參數： {arrayFormStr}</div>}
        {arrayFormResponseEncrypt && <div>返回結果：{arrayFormResponseEncrypt}</div>}
        {arrayFormResponseDecryptStr && <div>返回結果 解密：{arrayFormResponseDecryptStr}</div>}
      </Card>
    </div>
  );
};

export default ApiEncryptPage;
