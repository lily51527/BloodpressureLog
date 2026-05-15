package idv.wennyli.bloodpressurelog.data.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test

class DataStateTest {

    /** DataState.Loading 應為單一實例，確保不會建立重複物件。 */
    @Test
    fun `Loading is singleton`() {
        assertThat(DataState.Loading).isSameInstanceAs(DataState.Loading)
    }

    /** DataState.Success 應正確保存傳入的資料值。 */
    @Test
    fun `Success holds data`() {
        val state = DataState.Success(42)
        assertThat(state.data).isEqualTo(42)
    }

    /** DataState.Success 應正確保存 List 型別的資料。 */
    @Test
    fun `Success with list holds data`() {
        val list = listOf("a", "b")
        val state = DataState.Success(list)
        assertThat(state.data).isEqualTo(list)
    }

    /** DataState.Error 應同時保存例外物件與對應的錯誤訊息。 */
    @Test
    fun `Error holds throwable and message`() {
        val throwable = RuntimeException("test error")
        val state = DataState.Error(throwable, "test error")
        assertThat(state.throwable).isSameInstanceAs(throwable)
        assertThat(state.message).isEqualTo("test error")
    }

    /** DataState.Success 應能包裝 Unit 作為無回傳值操作成功的狀態。 */
    @Test
    fun `Success with Unit`() {
        val state = DataState.Success(Unit)
        assertThat(state.data).isEqualTo(Unit)
    }
}
