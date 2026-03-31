/**
 * Account Center - Personal Profile
 *
 * Corresponds to Vue's account/components/center/index.vue (307L)
 * Displays and edits user profile information with avatar upload.
 */
import React, { useCallback, useEffect, useState } from 'react';
import { Form, Input, Select, Button, Upload, message, Spin, Row, Col } from 'antd';
import { LoadingOutlined, PlusOutlined } from '@ant-design/icons';
import { getLoginInfo } from '@/api/system/login.api';
import { employeeApi } from '@/api/system/employee-api';
import { fileApi } from '@/api/support/file-api';
import type { UploadProps } from 'antd';

const ACCEPT_FILE_TYPES = '.jpg,.jpeg,.png,.gif';
const MAX_FILE_SIZE_MB = 10;
const FILE_FOLDER_COMMON = 1;

const AccountCenter: React.FC = () => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [avatarUrl, setAvatarUrl] = useState<string | undefined>();
  const [avatarLoading, setAvatarLoading] = useState(false);

  const loadProfile = useCallback(async () => {
    setLoading(true);
    try {
      const res = await getLoginInfo();
      if (res.code === 1 && res.data) {
        form.setFieldsValue(res.data);
        setAvatarUrl(res.data.avatar);
      }
    } finally {
      setLoading(false);
    }
  }, [form]);

  useEffect(() => {
    loadProfile();
  }, [loadProfile]);

  const handleSubmit = async () => {
    const values = await form.validateFields();
    setSubmitting(true);
    try {
      await employeeApi.updateCenter(values);
      message.success('更新成功');
      await loadProfile();
    } catch (error: any) {
      if (error.errorFields) {
        message.error('参数验证错误，请仔细填写表单数据!');
      }
    } finally {
      setSubmitting(false);
    }
  };

  /** Avatar upload: validate before upload */
  const beforeUpload: UploadProps['beforeUpload'] = (file) => {
    const suffixIndex = file.name.lastIndexOf('.');
    const fileSuffix = file.name.substring(suffixIndex <= -1 ? 0 : suffixIndex);
    if (ACCEPT_FILE_TYPES.indexOf(fileSuffix) === -1) {
      message.error(`只支持上传 ${ACCEPT_FILE_TYPES.replaceAll(',', ' ')} 格式的文件`);
      return false;
    }
    const isLimitSize = file.size / 1024 / 1024 < MAX_FILE_SIZE_MB;
    if (!isLimitSize) {
      message.error(`单个文件大小必须小于 ${MAX_FILE_SIZE_MB} Mb`);
      return false;
    }
    return true;
  };

  /** Avatar upload: custom request */
  const customRequest: UploadProps['customRequest'] = async (options) => {
    setAvatarLoading(true);
    try {
      const formData = new FormData();
      formData.append('file', options.file as File);
      const res = await fileApi.uploadFile(formData, FILE_FOLDER_COMMON);
      if (res.code === 1 && res.data) {
        setAvatarUrl(res.data.fileUrl);
        await employeeApi.updateAvatar({ avatar: res.data.fileKey });
        message.success('更新成功');
        await loadProfile();
      }
    } catch {
      message.error('头像上传失败');
    } finally {
      setAvatarLoading(false);
    }
  };

  return (
    <Spin spinning={loading}>
      <div style={{ fontSize: 20, marginBottom: 20 }}>个人中心</div>
      <Row>
        <Col flex="350px">
          <Form form={form} layout="vertical" style={{ maxWidth: 350 }}>
            <Form.Item label="登录账号" name="loginName">
              <Input disabled />
            </Form.Item>
            <Form.Item label="部门" name="departmentName">
              <Input disabled />
            </Form.Item>
            <Form.Item
              label="员工名称"
              name="actualName"
              rules={[
                { required: true, message: '姓名不能为空' },
                { max: 30, message: '姓名不能大于30个字符' },
              ]}
            >
              <Input placeholder="请输入员工名称" />
            </Form.Item>
            <Form.Item label="性别" name="gender" rules={[{ required: true, message: '性别不能为空' }]}>
              <Select
                placeholder="请选择性别"
                options={[
                  { label: '男', value: 1 },
                  { label: '女', value: 2 },
                  { label: '未知', value: 0 },
                ]}
              />
            </Form.Item>
            <Form.Item
              label="手机号码"
              name="phone"
              rules={[
                { required: true, message: '手机号不能为空' },
                { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码' },
              ]}
            >
              <Input placeholder="请输入手机号码" />
            </Form.Item>
            <Form.Item label="邮箱" name="email" rules={[{ required: true, message: '请输入邮箱' }]}>
              <Input placeholder="请输入邮箱" />
            </Form.Item>
            <Form.Item label="职务" name="positionId">
              <Input placeholder="职务" disabled />
            </Form.Item>
            <Form.Item label="备注" name="remark">
              <Input.TextArea rows={4} placeholder="请输入备注" />
            </Form.Item>
            <Form.Item>
              <Button type="primary" onClick={handleSubmit} loading={submitting}>
                更新个人信息
              </Button>
            </Form.Item>
          </Form>
        </Col>
        <Col flex="auto" style={{ paddingLeft: 80 }}>
          <div style={{ marginBottom: 8 }}>头像</div>
          <Upload
            name="avatar"
            listType="picture-card"
            showUploadList={false}
            customRequest={customRequest}
            beforeUpload={beforeUpload}
            accept={ACCEPT_FILE_TYPES}
          >
            {avatarUrl ? (
              <img src={avatarUrl} alt="avatar" style={{ width: '100%', height: '100%', objectFit: 'cover', borderRadius: '50%' }} />
            ) : (
              <div>
                {avatarLoading ? <LoadingOutlined /> : <PlusOutlined />}
                <div style={{ marginTop: 8 }}>上传头像</div>
              </div>
            )}
          </Upload>
        </Col>
      </Row>
    </Spin>
  );
};

export default AccountCenter;
