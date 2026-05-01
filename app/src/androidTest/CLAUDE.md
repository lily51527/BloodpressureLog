# UI Test Guidelines (Instrumented)

此目錄為 Instrumented UI Test（`./gradlew connectedAndroidTest`），需要連接裝置或模擬器。

## 原則

- **測試 stateless composable**：直接傳入 `uiState`，避免依賴 Hilt 或 ViewModel
- **用使用者看到的文字找元素**：`onNodeWithText("新增紀錄")`，不用內部 tag/ID
- **只驗證使用者能感知的事**：畫面有沒有顯示、互動後有沒有變化
- **測試行為，不測樣式**：不驗證顏色、字體大小、間距等視覺細節
- **優先測試狀態切換**：Idle / Loading / Success / Error 各自應顯示與隱藏的元素
- **非同步狀態變化**：用 `waitUntil` 等待節點出現，避免 race condition

## 標準結構

```kotlin
@RunWith(AndroidJUnit4::class)
class YourScreenTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `should show record list when data loaded`() {
        // Arrange
        val uiState = RecordUiState.Success(records = fakeRecords)

        // Act
        composeTestRule.setContent {
            RecordScreen(uiState = uiState, onAddClick = {})
        }

        // Assert
        composeTestRule.onNodeWithText("收縮壓").assertIsDisplayed()
    }
}
```

## 測試對象

- 血壓紀錄列表的顯示與空狀態
- 新增紀錄表單的輸入驗證提示
- 血壓等級顏色標示是否隨數值變化
- Loading / Error 狀態的畫面切換
