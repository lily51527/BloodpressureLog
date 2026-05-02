# Unit Test Guidelines

此目錄為 JVM Unit Test（`./gradlew testDebugUnitTest`）。

## 原則

- **一個測試只驗證一件事**：失敗時能立刻定位問題
- **AAA 結構**：Arrange（準備）→ Act（執行）→ Assert（驗證），三段間留空行
- **測試名稱說明情境與預期結果**：用 backtick 包裹描述句，例如：
  `` `getRecords should return empty list when no data exists` ``
- **測試間彼此獨立**：用 `@Before` 重置狀態，執行順序不影響結果
- **只測自己寫的邏輯**：用 mock 隔離 Firebase、網路等外部依賴
- **涵蓋邊界條件**：正常輸入、空值/缺欄位、例外三種情境
- **不為覆蓋率而寫**：測試邏輯行為，而非讓每一行程式碼都被執行過

## 測試對象

- ViewModel 的狀態轉換邏輯
- Repository 的資料轉換 / 錯誤處理
- 工具類（DateUtils、TimeSlot.auto() 等純函數）
- 血壓等級判斷邏輯（BloodPressureLevel）

## 有效測試啟發法

### FIRST — 測試本身的品質標準
- **Fast**：測試執行要快，不依賴網路或真實 Firebase
- **Independent**：每個測試互不依賴，可單獨執行
- **Repeatable**：任何環境、任何時間執行結果都相同
- **Self-Validating**：測試本身判斷通過或失敗，不需人工檢查輸出
- **Timely**：跟著功能同步撰寫，不拖到事後補

### CORRECT — 邊界條件檢查清單
- **Conformance**：輸入格式是否符合預期（如收縮壓必須為正整數）
- **Ordering**：資料順序是否正確（紀錄列表應依時間降冪排列）
- **Range**：數值是否在合理範圍內（收縮壓 0 或 999 的極端值）
- **Reference**：外部依賴是否存在（Repository 回傳 null 時的處理）
- **Existence**：空集合、空字串、null 的情境
- **Cardinality**：0 筆、1 筆、多筆紀錄各自的行為是否正確
- **Time**：時間相關邏輯（TimeSlot.auto() 在不同時段的判斷）

### RIGHT-BICEP — 測試涵蓋面檢查清單
- **Right Results**：正常輸入產生正確結果
- **Boundary Conditions**：邊界值（最大值、最小值、空值）
- **Inverse Relationships**：新增後再查詢，結果應一致
- **Cross-check Results**：用不同方式驗證同一結果
- **Force Error Conditions**：強制觸發錯誤（網路失敗、Firestore 權限拒絕）
- **Performance Characteristics**：大量資料下 Flow 的表現（視需要）

### A TRIP — 測試整體目標
- **Automatic**：CI 自動執行，不需手動觸發
- **Thorough**：涵蓋正常、邊界、錯誤三類情境
- **Repeatable**：結果穩定，不因時間或環境改變
- **Independent**：測試之間零耦合
- **Professional**：測試程式碼與產品程式碼同等品質，命名清晰、結構整齊

## 測試檔案位置

production 檔案路徑對應到 `app/src/test/` 下相同的 package 路徑：

| Production | Test |
|-----------|------|
| `app/src/main/.../RecordViewModel.kt` | `app/src/test/.../RecordViewModelTest.kt` |
| `app/src/main/.../BloodPressureRepositoryImpl.kt` | `app/src/test/.../BloodPressureRepositoryImplTest.kt` |
| `app/src/main/.../utils/DateUtils.kt` | `app/src/test/.../utils/DateUtilsTest.kt` |

## 依照實作類型選擇測試模式

**純函式 / Utility**：直接呼叫，Assert 輸出值。

**ViewModel**：mock Repository，用 `runTest` + `UnconfinedTestDispatcher`，Assert `uiState` 的值。

**Repository**：mock Firestore 相關依賴（不使用真實 Firebase），用 `callbackFlow` 搭配 Turbine 驗證 Flow 發射的值。

## 工具

- **MockK**：mock Firebase / Repository 等外部依賴
- **Turbine**：測試 `Flow` 發出的值序列
- **Truth**：`assertThat(result).isEqualTo(expected)`
- **Coroutine Test**：`runTest` + `UnconfinedTestDispatcher`
