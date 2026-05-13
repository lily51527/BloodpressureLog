package idv.wennyli.bloodpressurelog.data.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test

class DataStateTest {

    @Test
    fun `Loading is singleton`() {
        assertThat(DataState.Loading).isSameInstanceAs(DataState.Loading)
    }

    @Test
    fun `Success holds data`() {
        val state = DataState.Success(42)
        assertThat(state.data).isEqualTo(42)
    }

    @Test
    fun `Success with list holds data`() {
        val list = listOf("a", "b")
        val state = DataState.Success(list)
        assertThat(state.data).isEqualTo(list)
    }

    @Test
    fun `Error holds throwable and message`() {
        val throwable = RuntimeException("test error")
        val state = DataState.Error(throwable, "test error")
        assertThat(state.throwable).isSameInstanceAs(throwable)
        assertThat(state.message).isEqualTo("test error")
    }

    @Test
    fun `Success with Unit`() {
        val state = DataState.Success(Unit)
        assertThat(state.data).isEqualTo(Unit)
    }
}
