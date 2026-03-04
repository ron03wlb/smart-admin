<template>
  <div class="mfa-container">
    <a-card title="多因素認證（MFA）" :bordered="false">
      <!-- MFA 狀態顯示 -->
      <a-alert
        v-if="mfaStatus.mfaEnabled"
        message="MFA 已啟用"
        description="您的帳號已啟用多因素認證，每次登入時需要輸入 TOTP 驗證碼或備份碼。"
        type="success"
        show-icon
        style="margin-bottom: 24px"
      />
      <a-alert
        v-else
        message="MFA 未啟用"
        description="建議啟用多因素認證以增強帳號安全性。"
        type="warning"
        show-icon
        style="margin-bottom: 24px"
      />

      <!-- 備份碼管理區塊 (Week 3 Day 1-2 核心功能) -->
      <a-card
        v-if="mfaStatus.mfaEnabled"
        title="備份碼管理"
        :bordered="false"
        style="margin-bottom: 24px; background-color: #fafafa"
      >
        <a-space direction="vertical" style="width: 100%" :size="16">
          <!-- 剩餘數量顯示 -->
          <a-statistic
            title="剩餘備份碼數量"
            :value="backupCodeCount"
            suffix="/ 10"
            :value-style="{ color: backupCodeCount <= 2 ? '#cf1322' : '#3f8600' }"
          />

          <!-- 低備份碼警告 -->
          <a-alert
            v-if="backupCodeCount <= 2"
            message="備份碼即將用盡"
            :description="`您只剩下 ${backupCodeCount} 個備份碼，建議立即重新生成備份碼。`"
            type="error"
            show-icon
            closable
            style="margin-bottom: 16px"
          />

          <!-- 重新生成備份碼按鈕 -->
          <a-button type="primary" @click="showRegenerateModal" :loading="regenerating">
            <template #icon><ReloadOutlined /></template>
            重新生成備份碼
          </a-button>

          <a-typography-text type="secondary">
            <InfoCircleOutlined /> 重新生成備份碼需要輸入當前的 TOTP 驗證碼，舊備份碼將會失效。
          </a-typography-text>
        </a-space>
      </a-card>

      <!-- MFA 設定操作 -->
      <a-space :size="16">
        <a-button v-if="!mfaStatus.mfaEnabled" type="primary" @click="startSetup">
          <template #icon><SafetyOutlined /></template>
          啟用 MFA
        </a-button>
        <a-button v-else danger @click="showDisableModal">
          <template #icon><StopOutlined /></template>
          禁用 MFA
        </a-button>
      </a-space>
    </a-card>

    <!-- 重新生成備份碼 Modal -->
    <a-modal
      v-model:open="regenerateModalVisible"
      title="重新生成備份碼"
      @ok="handleRegenerateBackupCodes"
      @cancel="regenerateModalVisible = false"
      :confirm-loading="regenerating"
      ok-text="確認重新生成"
      cancel-text="取消"
    >
      <a-form :model="regenerateForm" layout="vertical">
        <a-alert
          message="安全驗證"
          description="為確保安全，請輸入您的 Google Authenticator 中顯示的 6 位 TOTP 驗證碼。"
          type="info"
          show-icon
          style="margin-bottom: 16px"
        />
        <a-form-item label="TOTP 驗證碼" required>
          <a-input
            v-model:value="regenerateForm.totpToken"
            placeholder="請輸入 6 位 TOTP 驗證碼"
            maxlength="6"
            style="width: 200px"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 禁用 MFA Modal -->
    <a-modal
      v-model:open="disableModalVisible"
      title="禁用多因素認證"
      @ok="handleDisableMfa"
      @cancel="disableModalVisible = false"
      :confirm-loading="disabling"
      ok-text="確認禁用"
      cancel-text="取消"
      ok-type="danger"
    >
      <a-form :model="disableForm" layout="vertical">
        <a-alert
          message="警告"
          description="禁用 MFA 將降低您的帳號安全性。為確保安全，請輸入您的 TOTP 驗證碼。"
          type="warning"
          show-icon
          style="margin-bottom: 16px"
        />
        <a-form-item label="TOTP 驗證碼" required>
          <a-input
            v-model:value="disableForm.totpToken"
            placeholder="請輸入 6 位 TOTP 驗證碼"
            maxlength="6"
            style="width: 200px"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 新備份碼顯示 Modal -->
    <a-modal
      v-model:open="newBackupCodesModalVisible"
      title="新的備份碼"
      :footer="null"
      :closable="false"
      :maskClosable="false"
      width="600px"
    >
      <a-alert
        message="請妥善保存這些備份碼"
        description="這些備份碼僅顯示一次，請立即下載或列印保存。每個備份碼只能使用一次。"
        type="warning"
        show-icon
        style="margin-bottom: 16px"
      />
      <a-list :data-source="newBackupCodes" bordered size="small" style="margin-bottom: 16px">
        <template #renderItem="{ item, index }">
          <a-list-item>
            <a-typography-text code strong>{{ index + 1 }}. {{ item }}</a-typography-text>
          </a-list-item>
        </template>
      </a-list>
      <a-space>
        <a-button type="primary" @click="downloadBackupCodes">
          <template #icon><DownloadOutlined /></template>
          下載備份碼
        </a-button>
        <a-button @click="closeBackupCodesModal">我已保存，關閉</a-button>
      </a-space>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { message } from 'ant-design-vue';
import { mfaApi } from '/@/api/system/mfa-api';
import {
  SafetyOutlined,
  StopOutlined,
  ReloadOutlined,
  InfoCircleOutlined,
  DownloadOutlined,
} from '@ant-design/icons-vue';

// MFA 狀態
const mfaStatus = ref({
  mfaEnabled: false,
  mfaType: 'TOTP',
  backupCodesGenerated: false,
  remainingBackupCodes: 0,
  lastVerifiedAt: null,
  enforcedByRole: false,
  needRegenerateBackupCodes: false,
  qrCodeConfirmed: false,
});

// 備份碼剩餘數量
const backupCodeCount = ref(0);

// 重新生成備份碼相關
const regenerateModalVisible = ref(false);
const regenerating = ref(false);
const regenerateForm = ref({
  totpToken: '',
});

// 禁用 MFA 相關
const disableModalVisible = ref(false);
const disabling = ref(false);
const disableForm = ref({
  totpToken: '',
});

// 新備份碼顯示
const newBackupCodesModalVisible = ref(false);
const newBackupCodes = ref([]);

// 頁面加載時獲取 MFA 狀態
onMounted(async () => {
  await fetchMfaStatus();
  if (mfaStatus.value.mfaEnabled) {
    await fetchBackupCodeCount();
  }
});

/**
 * 獲取 MFA 狀態
 */
async function fetchMfaStatus() {
  try {
    const res = await mfaApi.getStatus();
    if (res.data && res.data.ok) {
      mfaStatus.value = res.data.data;
    }
  } catch (error) {
    message.error('獲取 MFA 狀態失敗');
  }
}

/**
 * 獲取剩餘備份碼數量
 */
async function fetchBackupCodeCount() {
  try {
    const res = await mfaApi.getBackupCodeCount();
    if (res.data && res.data.ok) {
      backupCodeCount.value = res.data.data;
    }
  } catch (error) {
    message.error('獲取備份碼數量失敗');
  }
}

/**
 * 啟動 MFA 設定（暫未實現完整設定流程）
 */
function startSetup() {
  message.info('MFA 完整設定流程將在後續版本實現');
}

/**
 * 顯示重新生成備份碼 Modal
 */
function showRegenerateModal() {
  regenerateForm.value.totpToken = '';
  regenerateModalVisible.value = true;
}

/**
 * 處理重新生成備份碼
 */
async function handleRegenerateBackupCodes() {
  if (!regenerateForm.value.totpToken || regenerateForm.value.totpToken.length !== 6) {
    message.error('請輸入 6 位 TOTP 驗證碼');
    return;
  }

  regenerating.value = true;
  try {
    const res = await mfaApi.regenerateBackupCodes(regenerateForm.value.totpToken);
    if (res.data && res.data.ok) {
      newBackupCodes.value = res.data.data.backupCodes;
      regenerateModalVisible.value = false;
      newBackupCodesModalVisible.value = true;
      message.success('備份碼重新生成成功');
      // 重新獲取數量
      await fetchBackupCodeCount();
    } else {
      message.error(res.data.msg || '重新生成備份碼失敗');
    }
  } catch (error) {
    message.error('重新生成備份碼失敗');
  } finally {
    regenerating.value = false;
  }
}

/**
 * 顯示禁用 MFA Modal
 */
function showDisableModal() {
  disableForm.value.totpToken = '';
  disableModalVisible.value = true;
}

/**
 * 處理禁用 MFA
 */
async function handleDisableMfa() {
  if (!disableForm.value.totpToken || disableForm.value.totpToken.length !== 6) {
    message.error('請輸入 6 位 TOTP 驗證碼');
    return;
  }

  disabling.value = true;
  try {
    const res = await mfaApi.setupDisable(disableForm.value.totpToken);
    if (res.data && res.data.ok) {
      disableModalVisible.value = false;
      message.success('MFA 已成功禁用');
      // 重新獲取狀態
      await fetchMfaStatus();
      backupCodeCount.value = 0;
    } else {
      message.error(res.data.msg || '禁用 MFA 失敗');
    }
  } catch (error) {
    message.error('禁用 MFA 失敗');
  } finally {
    disabling.value = false;
  }
}

/**
 * 下載備份碼
 */
function downloadBackupCodes() {
  const content = newBackupCodes.value.map((code, index) => `${index + 1}. ${code}`).join('\n');
  const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = `MFA_Backup_Codes_${new Date().toISOString().split('T')[0]}.txt`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
  message.success('備份碼已下載');
}

/**
 * 關閉備份碼 Modal
 */
function closeBackupCodesModal() {
  newBackupCodesModalVisible.value = false;
  newBackupCodes.value = [];
}
</script>

<style lang="less" scoped>
.mfa-container {
  :deep(.ant-card-head-title) {
    font-weight: 600;
  }
}
</style>
