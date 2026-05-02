# BloodPressureLog

給家人使用的血壓紀錄 Android app，支援每日多筆紀錄、時間段自動偵測、血壓等級標示，並即時同步至 Firebase Firestore。

---

## 功能規劃

### 登入
- Email / 密碼登入（release 與 debug 皆支援）
- 匿名登入（僅 debug build，供開發測試使用）
- 設計考量：使用者可能換手機，需保留帳號資料，故正式版不提供匿名登入

### 血壓紀錄
- 新增、編輯、刪除血壓紀錄
- 每筆紀錄欄位：收縮壓、舒張壓、脈搏、備註
- 時間段（早/中/晚）由系統時間自動偵測，無需手動選擇
- 備註欄位供使用者自由填寫（例如：是否服藥）

### 血壓等級標示（WHO 標準）

| 等級 | 收縮壓 (mmHg) | 舒張壓 (mmHg) | UI 顏色 |
|------|--------------|--------------|---------|
| 正常 | < 120 | < 80 | 綠色 |
| 正常偏高 | 120–129 | < 80 | 黃色 |
| 第一期高血壓 | 130–139 | 80–89 | 橘色 |
| 第二期高血壓 | ≥ 140 | ≥ 90 | 紅色 |

### 趨勢圖表
- 顯示一段時間內的收縮壓 / 舒張壓趨勢

### 設定
- 帳號管理（登出）

---

## 架構

**MVVM + Hilt DI + Jetpack Compose**，targeting Android 7.0+（minSdk 24）。

### 套件結構

```
idv.wennyli.bloodpressurelog/
├── data/
│   ├── model/           # 資料類別（BloodPressureRecord、TimeSlot 等）
│   └── repository/      # Repository 介面與實作
├── di/                  # Hilt modules（AppModule、ConfigModule）
├── ui/
│   ├── navigation/      # Navigation graphs
│   ├── view/            # 各功能畫面，每個畫面包含 ViewModel + Composable
│   │   ├── login/
│   │   ├── record/      # 血壓紀錄列表 + 新增/編輯
│   │   ├── trends/      # 趨勢圖表
│   │   └── settings/
│   └── theme/           # Material3 顏色、字體、主題
└── utils/               # 共用工具（DateUtils、FirestorePaths 等）
```

### 資料流

```
Composable
    ↕ observes StateFlow
ViewModel
    ↕ calls / collects Flow
Repository (interface)
    ↕ implements
BloodPressureRepositoryImpl
    ↕ addSnapshotListener
Firebase Firestore
```

- ViewModel 透過 Repository 取得 `Flow<List<BloodPressureRecord>>`，轉為 `StateFlow` 供 Composable 訂閱
- Firestore 使用 `addSnapshotListener` 即時同步，無本地 Room DB
- Hilt 管理所有依賴注入；`BloodPressureApplication` 為 `@HiltAndroidApp` 進入點

### Firebase 路徑結構

```
artifacts/{appId}/users/{userId}/
  └── bloodPressures/    # 血壓紀錄
```

`appId` 由 `BuildConfig.APP_ID` 注入，debug / release 使用不同值隔離資料。

---

## 技術棧

| 層級 | 套件 |
|------|------|
| UI | Jetpack Compose + Material3 |
| 導航 | Navigation Compose |
| DI | Hilt（KSP） |
| 後端 | Firebase Auth + Firestore |
| 語言 | Kotlin / JVM 17 |
| 單元測試 | JUnit4 + MockK + Turbine + Truth |
| UI 測試 | Compose Test（createComposeRule） |

---

## 開發環境與建置

### 必要條件

- Android Studio Meerkat 以上
- JDK 17
- Firebase 專案（設定 `google-services.json`）

### 建置指令

```bash
# 建置 debug APK
./gradlew assembleDebug

# 安裝至裝置
./gradlew installDebug

# 執行 unit test
./gradlew testDebugUnitTest

# 執行單一測試類別
./gradlew testDebugUnitTest --tests "idv.wennyli.bloodpressurelog.YourTestClass"

# 執行 UI test（需連接裝置或模擬器）
./gradlew connectedAndroidTest

# Lint 檢查
./gradlew lint
```

---

## 測試策略

### Unit Test（`app/src/test/`）

不依賴真實 Firebase，全以 mock 隔離外部依賴：

- **純函式 / Utility**：直接呼叫，斷言輸出值
- **ViewModel**：mock Repository，用 `runTest` + `UnconfinedTestDispatcher`，驗證 `uiState`
- **Repository**：mock Firestore，用 Turbine 驗證 Flow 發射的值序列

### UI Test（`app/src/androidTest/`）

- 測試 stateless Composable，直接傳入 `uiState`
- 用使用者可見的文字找元素（`onNodeWithText`），不用內部 tag
- 驗證 Loading / Success / Error 各狀態的畫面切換

---

## 設計原則

遵循 S.O.L.I.D：

- **S**：ViewModel 只處理 UI 狀態，Repository 只處理資料存取
- **O**：新增血壓等級時擴充 enum，不修改現有判斷邏輯
- **L**：`BloodPressureRepositoryImpl` 完整實作 `BloodPressureRepository` 介面
- **I**：每個 Repository 介面只定義該資料域的操作
- **D**：ViewModel 依賴 `BloodPressureRepository` 介面，由 Hilt 注入具體實作
