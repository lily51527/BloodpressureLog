# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 專案文件

本專案的開發紀錄、設計決策、功能規格等文件，統一存放於 Notion 的 **BloodPressureLog 開發紀錄** database。

## Architecture

**MVVM + Hilt DI + Jetpack Compose**，targeting Android 9.0+ (minSdk 28)。

### Package Structure

```
idv.wennyli.bloodpressurelog/
├── data/
│   ├── model/       # Data classes (BloodPressureRecord, TimeSlot, BloodPressureLevel, DataState, AuthException)
│   └── repository/  # Repository interfaces & implementations (BloodPressureRepository, AuthRepository)
├── di/              # Hilt modules (AppModule 負責 Firebase + Repository 綁定)
├── domain/
│   └── usecase/     # BuildChartDataUseCase（趨勢圖表資料轉換）、SaveBloodPressureRecordUseCase（新增/更新判斷）
├── ui/
│   ├── navigation/  # AppNavigation（含 BottomNav + NavHost）
│   ├── view/        # Feature screens，每個 feature 各有 ViewModel + Composable
│   │   ├── login/   # Login, Register, EmailVerification, ForgotPassword
│   │   ├── main/    # MainViewModel（監聽 auth state）
│   │   ├── record/  # 血壓紀錄列表 + 新增/編輯
│   │   └── trends/  # 趨勢圖表（Vico 折線圖）
│   └── theme/       # Material3 color, typography, theme
└── utils/           # DateUtils, FirebaseAuthErrorMapper, FirestorePaths, ResourceProvider
```

### Key Tech Stack

| Layer | Library |
|-------|---------|
| UI | Jetpack Compose + Material3 |
| Navigation | Navigation Compose |
| DI | Hilt (KSP) |
| Backend | Firebase Auth + Firestore |
| Charting | Vico (`com.patrykandpatrick.vico:compose-m3`) |
| Language | Kotlin / JVM 17 |

### Firebase 路徑結構

```
artifacts/{appId}/users/{userId}/
  └── bloodPressures/    # 血壓紀錄
```

`appId` 由 `BuildConfig.APP_ID` 注入，debug / release 使用不同值以隔離資料。路徑由 `FirestorePaths.bloodPressures(appId, userId)` 產生。

Firebase 設定檔（`firebase.json`、`firestore.rules`）位於獨立的 backend 專案：

```
/Users/wenyi_li/Backend Project/BloodPressureLog-app-backend/
├── firebase.json
└── firestore.rules
```

### Navigation Flow

```
Login ──► Register ──► EmailVerification ──► Login
     └──► ForgotPassword (pop back)
     └──► EmailVerification（已登入但未驗證時）

RecordList ◄──► AddEditRecord（新增傳 null，編輯傳 recordId）
RecordList ◄──► Trends（BottomNav 切換）
```

- `MainViewModel` 監聽 `authStateChanges`，用戶登出時強制導回 Login 並清除 back stack
- `AppNavigation` 以 `startLoggedIn` 參數決定初始路由（`record_list` 或 `login`）
- `NavHost` 套用 `consumeWindowInsets(innerPadding)` 防止內層 Scaffold 重複消費 window insets

### DataState

所有非同步操作以 `DataState<T>` 回傳：

```kotlin
sealed interface DataState<out T> {
    data object Loading : DataState<Nothing>
    data class Success<T>(val data: T) : DataState<T>
    data class Error(val throwable: Throwable, val message: String) : DataState<Nothing>
}
```

### Data Flow

- ViewModel 透過 Repository 取得 `Flow<DataState<List<BloodPressureRecord>>>`，轉為 `StateFlow` 供 Composable 訂閱
- Firestore 使用 `addSnapshotListener` 即時同步，無本地 Room DB
- Hilt 管理所有依賴注入；`BloodPressureApplication` 為 `@HiltAndroidApp` 進入點

### 登入策略

- **release build**：僅顯示 Email / 密碼登入
- **debug build**：Email / 密碼 + 匿名登入（供測試使用）

### 血壓等級標準（WHO）

| 等級 | 收縮壓 (mmHg) | 舒張壓 (mmHg) | UI 顏色 |
|------|--------------|--------------|---------|
| 正常 | < 120 | < 80 | 綠色 |
| 正常偏高 | 120–129 | < 80 | 黃色 |
| 第一期高血壓 | 130–139 | 80–89 | 橘色 |
| 第二期高血壓 | ≥ 140 | ≥ 90 | 紅色 |

判斷邏輯在 `bloodPressureLevel(systolic, diastolic)` 函式（`BloodPressureLevel.kt`）。

### 重要領域邏輯

- `BloodPressureRecord.timeSlot`：extension property，依 `recordedAt` 小時判斷時段（早上 6–11、下午 12–17、晚上 18–23、夜間其他）
- `BuildChartDataUseCase`：將 records 依日期分組、計算各日平均值，x 軸為相對於 startDate 的 dayIndex，並產生對應天數的 x 軸標籤（格式 `M/d`）
- `SaveBloodPressureRecordUseCase`：依 `recordId` 是否為 null 決定呼叫 `addRecord` 或 `updateRecord`，例外統一轉為 `SaveRecordResult.Error`
- `AuthException`：`AuthRepositoryImpl` 將 Firebase 例外轉換為本地化訊息後以此包裝拋出，ViewModel 直接取用 `message` 顯示

### ResourceProvider 模式

`ResourceProvider` 介面封裝 `context.getString()`，由 Hilt 注入 `ResourceProviderImpl`。ViewModel 依賴此介面而非 Android `Context`，unit test 時直接 mock 介面，不需 Robolectric。

### TrendsViewModel 資料流設計

`TrendsViewModel` 以三個獨立 `MutableStateFlow` 合流，任一異動都會觸發 `uiState` 重算：

```kotlin
combine(_recordsState, _selectedRange, _selectedMetric) { state, range, metric -> ... }
    .stateIn(started = SharingStarted.Eagerly, ...)
```

`TrendsUiState` 的 `isLoading` **預設值為 `true`**。在 Preview 或測試中手動建構 `TrendsUiState` 時，若只傳 `errorMessage` 卻不傳 `isLoading = false`，畫面會顯示 Loading 而非 Error 狀態。

### Vico 圖表注意事項

`CartesianValueFormatter` 回傳的字串**不可為空字串或純空白**（Vico 內部以 `isNotBlank()` 驗證，違反時拋出 `IllegalStateException`）。fallback 值應使用 `index.toString()` 而非 `" "`。

