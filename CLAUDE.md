# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 專案文件

本專案的開發紀錄、設計決策、功能規格等文件，統一存放於 Notion 的 **BloodPressureLog 開發紀錄** database。

## Build & Test Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run a single test class
./gradlew testDebugUnitTest --tests "idv.wennyli.bloodpressurelog.YourTestClass"

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Lint
./gradlew lint

# Clean
./gradlew clean
```

## Architecture

**MVVM + Hilt DI + Jetpack Compose**，targeting Android 7.0+ (minSdk 24)。

### Package Structure

```
idv.wennyli.bloodpressurelog/
├── data/
│   ├── model/       # Data classes (BloodPressureRecord, TimeSlot, etc.)
│   └── repository/  # Repository interfaces & implementations
├── di/              # Hilt modules (AppModule)
├── ui/
│   ├── navigation/  # Navigation graphs
│   ├── view/        # Feature screens, each with ViewModel + Composable
│   │   ├── login/   # Login, Register, EmailVerification, ForgotPassword
│   │   ├── record/  # 血壓紀錄列表 + 新增/編輯
│   │   └── trends/  # 趨勢圖表
│   └── theme/       # Material3 color, typography, theme
└── utils/           # 共用工具（DateUtils, FirestorePaths 等）
```

### Key Tech Stack

| Layer | Library |
|-------|---------|
| UI | Jetpack Compose + Material3 |
| Navigation | Navigation Compose |
| DI | Hilt (KSP) |
| Backend | Firebase Auth + Firestore |
| Language | Kotlin / JVM 17 |

### Firebase 路徑結構

```
artifacts/{appId}/users/{userId}/
  └── bloodPressures/    # 血壓紀錄
```

`appId` 由 `BuildConfig.APP_ID` 注入，debug / release 使用不同值以隔離資料。

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

- ViewModel 透過 Repository 取得 `Flow<List<BloodPressureRecord>>`，轉為 `StateFlow` 供 Composable 訂閱
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

## Test Pattern

所有 ViewModel 測試需使用 `MainDispatcherRule`（位於 `app/src/test/.../MainDispatcherRule.kt`）替換 Main Dispatcher：

```kotlin
@get:Rule
val mainDispatcherRule = MainDispatcherRule()
```

Repository 測試使用 fake 實作（需完整實作介面），不依賴真實 Firebase。
