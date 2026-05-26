# BloodPressureLog - 血壓紀錄

給家人使用的血壓紀錄 Android app，支援每日多筆紀錄、時間段自動偵測、血壓等級標示，並透過趨勢圖表追蹤長期變化，所有資料即時同步至 Firebase Firestore。

![Kotlin](https://img.shields.io/badge/Kotlin-2.0-purple)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-blue)
![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%7C%20Auth-orange)

---

## 截圖

| 血壓紀錄 | 新增記錄 | 趨勢圖表 |
|:---:|:---:|:---:|
| <img src="screenshots/record_list.png" width="200"/> | <img src="screenshots/add_record.png" width="200"/> | <img src="screenshots/trends.png" width="200"/> |

---

## 主要功能

- **血壓紀錄**：新增、編輯、刪除每日血壓，欄位含收縮壓、舒張壓、脈搏、備註
- **時間段自動偵測**：依系統時間自動歸類早上／下午／晚上／夜間，無需手動選擇
- **血壓等級標示（WHO 標準）**：正常（綠）、正常偏高（黃）、第一期（橘）、第二期高血壓（紅）
- **日期篩選**：指定起訖日期篩選歷史紀錄
- **趨勢圖表**：以折線圖呈現收縮壓 / 舒張壓的長期趨勢
- **帳號管理**：Email / 密碼登入；debug build 另支援匿名登入供測試使用

---

## 架構

採用 **MVVM + Hilt DI + Jetpack Compose**，targeting Android 9.0+（minSdk 28）：

```
UI（Composable）
    ↕ UiState / Event
ViewModel
    ↕
Repository
    ↕
Firebase（Firestore / Auth）
```

### 目錄結構

```
idv.wennyli.bloodpressurelog/
├── data/
│   ├── model/       # 資料類別（BloodPressureRecord、TimeSlot、BloodPressureLevel 等）
│   └── repository/  # Repository 介面與實作
├── di/              # Hilt modules（AppModule）
├── domain/
│   └── usecase/     # BuildChartDataUseCase、SaveBloodPressureRecordUseCase
├── ui/
│   ├── navigation/  # AppNavigation（BottomNav + NavHost）
│   ├── theme/       # Material3 顏色、字體、主題
│   └── view/        # 各功能畫面（Screen + ViewModel）
│       ├── login/
│       ├── record/  # 血壓紀錄列表 + 新增/編輯
│       └── trends/  # 趨勢圖表
└── utils/           # DateUtils、FirestorePaths、ResourceProvider 等
```

---

## 技術棧

| 層級 | 技術 |
|------|------|
| 語言 | Kotlin 2.x / JVM 17 |
| UI | Jetpack Compose + Material3 |
| 架構 | MVVM |
| 非同步 | Coroutines + Flow |
| 依賴注入 | Hilt（KSP） |
| 後端 | Firebase Auth / Firestore / Crashlytics |
| 圖表 | Vico（`compose-m3`） |
| 測試 | JUnit4 + MockK + Turbine + assertk |

---

## 測試

- **Unit Test**：涵蓋 ViewModel、Repository、UseCase 層，使用 MockK mock Firebase 依賴
- **UI Test（Instrumented）**：直接傳入 `uiState` 測試 stateless Composable，涵蓋 Loading / Success / Error 各狀態

