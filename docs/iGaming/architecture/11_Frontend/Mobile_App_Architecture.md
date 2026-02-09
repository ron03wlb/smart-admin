# Mobile App Architecture

> **Business Requirements**: [Mobile App Requirements](../../requirements/11_Frontend_Experience/Mobile_App_Requirements.md)
> **Canonical Source**: [source-archive/11_Frontend_CMS/11-04](../../source-archive/11_Frontend_CMS/11-04_Mobile_App_Architecture.md)
> **View Type**: Technical Architecture
> **Target Audience**: Architects, Mobile Developers

---

## 1. Framework Comparison

| Aspect | Flutter | React Native | Recommendation |
|--------|---------|--------------|----------------|
| Language | Dart | JavaScript/TypeScript | RN for frontend teams |
| Performance | Near-native | Slightly below Flutter | Flutter for game lobby |
| Hot Update | Not officially supported | CodePush (mature) | RN for iGaming hot update needs |
| Bundle Size | 15-20MB | 25-30MB | Flutter lighter |

## 2. Hot Update (CodePush)

```javascript
import codePush from "react-native-code-push";

const codePushOptions = {
  checkFrequency: codePush.CheckFrequency.ON_APP_RESUME,
  installMode: codePush.InstallMode.ON_NEXT_RESUME,
  minimumBackgroundDuration: 60 * 5, // 5 min background before update
};

export default codePush(codePushOptions)(App);
```

## 3. Offline Storage

```javascript
import { persistStore, persistReducer } from 'redux-persist';
import AsyncStorage from '@react-native-async-storage/async-storage';

const persistConfig = {
  key: 'root',
  storage: AsyncStorage,
  whitelist: ['user', 'gameHistory', 'promotions'],
};

const persistedReducer = persistReducer(persistConfig, rootReducer);
```

## 4. Push Notification Architecture

### 4.1 Deep Linking

```javascript
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

### 4.2 Anti-Disturbance Config

```javascript
const pushConfig = {
  maxDailyPushes: 5,
  quietHours: { start: "23:00", end: "09:00" },
  userPreference: true,
};
```

## 5. Performance Optimization

### 5.1 Image Caching & List Virtualization

```javascript
<FastImage
  source={{ uri: gameImage, priority: FastImage.priority.normal }}
  cacheControl="max-age=86400"
  resizeMode="cover"
/>

<FlatList
  data={games}
  renderItem={renderGame}
  windowSize={5}
  removeClippedSubviews={true}
  maxToRenderPerBatch={10}
/>
```

## 6. Security Implementation

### 6.1 Error Boundary

```javascript
class ErrorBoundary extends React.Component {
  componentDidCatch(error, errorInfo) {
    logErrorToService(error, errorInfo);
    this.setState({ hasError: true });
  }
}
```

### 6.2 Security Stack

- **APK Packing**: 360/Tencent hardening
- **Code Obfuscation**: ProGuard (Android) / Strip Symbols (iOS)
- **SSL Pinning**: Prevent MITM attacks
- **API Signature**: Request signing for tamper prevention
- **Local Encryption**: AES-256 for AsyncStorage
- **Keychain/KeyStore**: Token storage in system-level secure storage
