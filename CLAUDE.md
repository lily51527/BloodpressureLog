# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 套件安裝規範

禁止擅自安裝任何軟體或第三方套件，包含但不限於：

- Gradle 依賴（`build.gradle.kts` 或 `libs.versions.toml` 新增套件）
- 系統軟體（`brew install` 等）
- 任何其他工具或 CLI

若判斷需要安裝新套件，必須先向使用者說明：
1. 套件名稱
2. 安裝原因（用途是什麼）
3. 安裝方式

由使用者確認後才能執行安裝。

## 工作目錄規則

所有程式碼修改必須直接在專案主目錄（`/Users/wenyi_li/AndroidStudioProjects/BloodPressureLog`）進行，不可只修改 worktree（`.claude/worktrees/` 下的目錄）。若目前環境是在 worktree 內，請改以主目錄的絕對路徑操作檔案。

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
├── di/              # Hilt modules (AppModule, ConfigModule)
├── ui/
│   ├── navigation/  # Navigation graphs
│   ├── view/        # Feature screens, each with ViewModel + Composable
│   │   ├── login/
│   │   ├── record/  # 血壓紀錄列表 + 新增/編輯
│   │   ├── trends/  # 趨勢圖表
│   │   └── settings/
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

### Dependency Versions

統一在 `gradle/libs.versions.toml` 管理，禁止在 `build.gradle.kts` 內寫 inline 版本號。

## S.O.L.I.D 原則

### S — Single Responsibility Principle（單一職責）
每個類別只負責一件事。ViewModel 只處理 UI 狀態，Repository 只處理資料存取，不混用。

### O — Open/Closed Principle（開放封閉）
對擴充開放、對修改封閉。新增血壓等級或時間段時，擴充 enum / sealed class，不修改現有判斷邏輯。

### L — Liskov Substitution Principle（里氏替換）
實作類別可以完全替換介面。`BloodPressureRepositoryImpl` 必須完整實作 `BloodPressureRepository` 介面定義的行為，測試用的 fake repository 同理。

### I — Interface Segregation Principle（介面隔離）
介面只定義呼叫方實際需要的方法，不強迫實作不需要的功能。每個 Repository 介面只定義該資料域的操作，不合併成一個大介面。

### D — Dependency Inversion Principle（依賴反轉）
高層模組依賴介面，不依賴具體實作。ViewModel 依賴 `BloodPressureRepository` 介面，由 Hilt 注入具體實作，方便替換與測試。
