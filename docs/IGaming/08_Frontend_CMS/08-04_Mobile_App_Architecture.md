# 08-04 行動端 App 架構 (Mobile App Architecture)

## 1. 系統概述
為提供最佳的移動端體驗，平台需提供原生 (Native) 或 混合 (Hybrid) App。
核心挑戰在於 **"上架審核困難"** (AppStore/PlayStore 禁止賭博 App) 與 **"頻繁更新需求"**。

## 2. 技術選型

### 2.1 框架策略

推薦採用 **Flutter** 或 **React Native** 進行跨平台開發：

| 對比項 | Flutter | React Native | 選型建議 |
|--------|---------|--------------|----------|
| **語言** | Dart | JavaScript/TypeScript | RN 對前端團隊友好 |
| **性能** | ⭐⭐⭐⭐⭐ 接近原生 | ⭐⭐⭐⭐ 略遜 Flutter | 高性能遊戲選 Flutter |
| **熱更新** | ❌ 官方不支持（需自建） | ✅ CodePush 成熟 | iGaming 強需求選 RN |
| **UI 渲染** | Skia 自繪引擎 | 原生組件橋接 | 複雜動畫選 Flutter |
| **生態系統** | 🟡 成長中 | ✅ 成熟豐富 | 第三方插件多選 RN |
| **包體積** | 📦 15-20MB | 📦 25-30MB | Flutter 更輕量 |
| **調試體驗** | ✅ DevTools 強大 | ✅ Chrome DevTools | 兩者都優秀 |

**推薦方案**：
- **高性能需求（大型遊戲大廳）**：Flutter
- **快速迭代 + 熱更新（iGaming 常態）**：React Native + CodePush

### 2.2 上架策略 (Distribution)
1.  **iOS**:
    *   **Enterprise Certificate (企業簽)**：無需上架 AppStore，用戶信任證書即可安裝 (容易掉簽，需頻繁補簽)。
    *   **TestFlight**: 透過測試名義分發 (有效期 90 天)。
    *   **Super Signature (超級簽)**：利用開發者帳號的 device id 額度分發。
    *   **WebClip (偽 App)**：在桌面生成一個 PWA 書籤，點擊開啟 Safari (最安全，但體驗稍差)。
2.  **Android**:
    *   **APK 下載**: 官網直接提供 APK 包。
    *   **Google Play (馬甲包)**：偽裝成 "記帳軟體" 或 "新聞閱讀器" 上架，通過後台開關切換為博彩介面 (風險高，易被下架)。

---

## 3. 核心功能模組

### 3.1 基礎設施
*   **HttpDNS**: 防止 DNS 污染，確保 API 連線穩定。
*   **Domain Fronting (域前置)**: 隱藏真實後端 IP，抗封鎖。

### 3.2 原生功能 (Native Features)
*   **Biometrics**: 支援 FaceID / TouchID 快速登入。
*   **Push Notification**: 集成 FCM / APNS，接收營銷推送。
*   **Device Fingerprint**: 採集 IDFA / GAID / IMEI / AndroidID 用於風控。

### 3.3 熱更新機制 (Hot Update)

**更新流程**：
1.  App 啟動時檢查 version.json。
2.  若有新版本 (Patch)，背景下載 JS Bundle / Assets。
3.  下載完成後，提示用戶 "重啟生效" 或 下次啟動自動套用。
4.  **回滾機制**：若新包崩潰 (Crash)，自動還原至上一版。

**CodePush 配置範例 (React Native)**：
```javascript
// App.js
import codePush from "react-native-code-push";

const codePushOptions = {
  checkFrequency: codePush.CheckFrequency.ON_APP_RESUME,
  installMode: codePush.InstallMode.ON_NEXT_RESUME,
  minimumBackgroundDuration: 60 * 5, // 5 分鐘後台後更新
};

export default codePush(codePushOptions)(App);
```

**分階段發佈策略**：
- **Stage 1 (5% 用戶)**：灰度測試，監控崩潰率
- **Stage 2 (20% 用戶)**：若無異常，擴大範圍
- **Stage 3 (100% 用戶)**：全量發佈

### 3.4 離線支持 (Offline Mode)

**關鍵場景**：
- 玩家在地鐵、電梯等弱網環境下仍可瀏覽遊戲資訊
- 已登入用戶可查看歷史投注記錄（快取數據）

**技術實作**：
```javascript
// 使用 Redux Persist + AsyncStorage
import { persistStore, persistReducer } from 'redux-persist';
import AsyncStorage from '@react-native-async-storage/async-storage';

const persistConfig = {
  key: 'root',
  storage: AsyncStorage,
  whitelist: ['user', 'gameHistory', 'promotions'], // 僅快取必要數據
};

const persistedReducer = persistReducer(persistConfig, rootReducer);
```

**離線可用功能**：
- ✅ 查看遊戲大廳（快取遊戲列表）
- ✅ 瀏覽歷史投注記錄（最近 100 筆）
- ✅ 查看個人資料
- ❌ 下注、充值、提款（需網路連線）

---

---

## 4. 推送通知架構 (Push Notification)

### 4.1 多渠道整合

| 平台 | 服務商 | 用途 |
|------|--------|------|
| iOS | APNS (Apple Push) | 官方推送通道 |
| Android | FCM (Firebase Cloud Messaging) | Google 官方 |
| Android (中國) | 小米推送、華為 Push、OPPO Push | 提升國內送達率 |
| App 內推送 | WebSocket / MQTT | 即時通知（App 在前台時） |

### 4.2 推送分類與策略

**營銷推送**：
- 新活動上線（點擊率 ROI 追蹤）
- VIP 專屬優惠
- 限時促銷（倒計時提醒）

**交易推送**：
- 充值成功通知
- 提款審核狀態更新（待審核 → 已批准 → 已到賬）
- 大額中獎祝賀

**風控推送**：
- 異常登入提醒（新設備登入）
- 帳號安全警告

**推送頻率控制**：
```javascript
// 防打擾策略
const pushConfig = {
  maxDailyPushes: 5, // 每日最多 5 條營銷推送
  quietHours: { start: "23:00", end: "09:00" }, // 靜默時段
  userPreference: true, // 尊重用戶訂閱偏好
};
```

### 4.3 Deep Linking（深度連結）

**場景**：推送點擊後直接跳轉到活動頁面，而非僅打開 App 首頁。

**實作範例**：
```javascript
// 處理推送點擊事件
messaging().onNotificationOpenedApp(remoteMessage => {
  const { screen, params } = remoteMessage.data;

  switch(screen) {
    case 'promotion':
      navigation.navigate('PromotionDetail', { id: params.promotionId });
      break;
    case 'withdrawal':
      navigation.navigate('WithdrawalStatus', { id: params.withdrawalId });
      break;
  }
});
```

---

## 5. 性能優化策略

### 5.1 啟動速度優化

**目標**: 冷啟動 < 2 秒，熱啟動 < 0.5 秒

**優化手段**：
1. **延遲載入 (Lazy Loading)**：非首屏模組延遲加載
2. **預載入關鍵資源**：首頁 Banner、熱門遊戲圖標
3. **Hermes 引擎 (RN)**：提升 JavaScript 執行速度 30%+
4. **Bundle 拆分**：基礎包 + 業務包分離

### 5.2 記憶體管理

**常見問題**：
- 圖片快取過多導致 OOM (Out of Memory)
- WebView 記憶體洩漏

**解決方案**：
```javascript
// 圖片快取策略
<FastImage
  source={{ uri: gameImage, priority: FastImage.priority.normal }}
  cacheControl="max-age=86400" // 快取 24 小時
  resizeMode="cover"
/>

// 列表虛擬化
<FlatList
  data={games}
  renderItem={renderGame}
  windowSize={5} // 只渲染可見區域 + 上下各 2 屏
  removeClippedSubviews={true}
  maxToRenderPerBatch={10}
/>
```

### 5.3 網路優化

- **HTTP/2 多路複用**：減少 TCP 連線數
- **圖片 WebP 格式**：相同質量下體積減少 30%
- **CDN 加速**：遊戲圖標、Banner 存放 CDN
- **請求合併**：多個 API 請求批量處理

---

## 6. 安全性

### 6.1 代碼保護
*   **加殼 (Packing)**：防止 APK 被反編譯與篡改 (如 360加固 / 騰訊樂固)。
*   **代碼混淆**：ProGuard (Android) / Strip Symbols (iOS)
*   **Root/Jailbreak Detection**: 檢測到設備已越獄/Root，強制退出或限制高風險操作。

### 6.2 通訊安全
*   **SSL Pinning**: 防止中間人攻擊 (Man-in-the-Middle) 抓包。
*   **API 簽名驗證**：每個請求帶簽名，防止參數篡改
*   **請求加密**：敏感欄位 AES-256 加密傳輸

### 6.3 數據安全
- **本地數據加密**：AsyncStorage 數據 AES 加密
- **Keychain/KeyStore**：敏感數據（Token）存儲在系統級安全儲存
- **截圖防護**：支付、個人資料頁面禁用截圖

---

## 7. 用戶體驗最佳實踐

### 7.1 適配策略
- **螢幕尺寸**：支持 4.7" - 6.7" 主流尺寸
- **安全區域**：適配 iPhone 瀏海、Android 打孔屏
- **橫豎屏**：遊戲頁面支持橫屏（特別是老虎機、真人遊戲）

### 7.2 動畫與回饋
- **骨架屏 (Skeleton Screen)**：載入時顯示內容佔位
- **觸覺回饋 (Haptic Feedback)**：重要操作給予震動回饋
- **樂觀更新 (Optimistic UI)**：充值後立即顯示更新餘額（後台確認後同步）

### 7.3 錯誤處理
```javascript
// 全局錯誤邊界
class ErrorBoundary extends React.Component {
  componentDidCatch(error, errorInfo) {
    // 記錄錯誤到 Sentry / Firebase Crashlytics
    logErrorToService(error, errorInfo);

    // 顯示友好錯誤頁面
    this.setState({ hasError: true });
  }
}
```

---

## 8. 測試策略

### 8.1 測試金字塔

| 測試類型 | 工具 | 覆蓋率目標 |
|---------|------|-----------|
| **E2E 測試** | Detox (RN) / Flutter Driver | 關鍵流程 100% |
| **整合測試** | Jest + React Testing Library | 核心組件 80% |
| **單元測試** | Jest | 工具函數 90% |

### 8.2 設備覆蓋測試
- iOS: iPhone SE (小屏) / iPhone 14 Pro (瀏海) / iPad
- Android: 三星 S22 (旗艦) / 小米紅米 (中端) / 華為 (HMS 無 GMS)

---

## 📚 相關文檔

### 前置依賴
- [08-01 前端佈局引擎](./08-01_Frontend_Layout_Engine.md) - 組件設計規範
- [08-05 本地化系統](./08-05_Localization_System.md) - 多語言實作

### 技術參考
- [12-03 網關架構](../12_Technical_Operations/12-03_Gateway_Architecture.md) - API 限流與安全
- [09-03 數據安全標準](../09_System_Security/09-03_Data_Security_Standard.md) - 加密規範

### 業務整合
- [01-01 玩家賬戶系統](../01_Player_Center/01-01_Player_Account_System.md) - 登入與 MFA
- [02-06 統一錢包模型](../02_Finance_Center/02-06_Unified_Wallet_Model.md) - 餘額顯示

---

**最後更新**: 2026-01-27
**維護者**: Frontend Team
