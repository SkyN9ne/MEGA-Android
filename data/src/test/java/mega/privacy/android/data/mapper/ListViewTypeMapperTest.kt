package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.preference.ListViewType
import org.junit.Test

/**
 * Test class for [ListViewTypeMapper]
 */
class ListViewTypeMapperTest {
    private val underTest: ListViewTypeMapper = { ListViewType(it) }

    @Test
    fun `test that null id returns null`() {
        assertThat(underTest(null)).isNull()
    }

    @Test
    fun `test that values are mapped by id`() {
        ListViewType.values().forEach {
            assertThat(underTest(it.id)).isEqualTo(it)
        }
    }

    @Test
    fun `test that when the value is invalid, null is returned`() {
        val invalidId = ListViewType.values().maxOf { it.id } + 1
        assertThat(underTest(invalidId)).isNull()
    }
}