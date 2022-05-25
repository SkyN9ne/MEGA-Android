package test.mega.privacy.android.app.fragments.homepage.photos

import android.text.Spanned
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.homepage.photos.DateCardsProvider
import mega.privacy.android.app.gallery.data.GalleryCard
import mega.privacy.android.app.gallery.fragment.GroupingLevel
import mega.privacy.android.app.utils.StringResourcesUtils
import mega.privacy.android.app.utils.StringUtils.toSpannedHtmlText
import mega.privacy.android.app.utils.wrapper.FileUtilWrapper
import nz.mega.sdk.MegaNode
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.*
import timber.log.Timber
import java.io.File
import java.time.LocalDateTime
import java.time.OffsetDateTime

class DateCardsProviderTest() {
    private val previewFolder = File("path")

    private lateinit var underTest: DateCardsProvider

    private val fileUtilWrapper = mock<FileUtilWrapper>()

    @Before
    fun setUp() {
        underTest = DateCardsProvider(previewFolder = previewFolder, fileUtil = fileUtilWrapper)
    }

    @Test
    fun `test that all lists are empty if input is empty`() {
        underTest.processNodes(emptyList())

        assertThat(underTest.getDays()).isEmpty()
        assertThat(underTest.getMonths()).isEmpty()
        assertThat(underTest.getYears()).isEmpty()
        assertThat(underTest.getNodesWithoutPreview()).isEmpty()
    }

    @Test
    fun `test that a card is returned for every day`() {

        val numberOfYears = 1
        val numberOfDaysPerMonth = 20
        val numberOfMonthsPerYear = 1
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.processNodes(nodes)
        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)
    }

    @Test
    fun `test that multiple dates on the same day increases the item count`() {
        val numberOfYears = 1
        val numberOfDaysPerMonth = 1
        val numberOfMonthsPerYear = 1
        val numberOfDuplicateDays = 20
        val nodes = (1..numberOfDuplicateDays).map {
            getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)
        }.flatten()

        underTest.processNodes(nodes)
        val days = underTest.getDays()
        assertThat(days).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)
        assertThat(days.last().numItems).isEqualTo(numberOfDuplicateDays - 1)
    }

    @Test
    fun `test that only one month card is returned if all dates are in the same month`() {
        val numberOfYears = 1
        val numberOfDaysPerMonth = 20
        val numberOfMonthsPerYear = 1
        val nodes = getNodes(numberOfYears = numberOfYears, numberOfMonthsPerYear = numberOfMonthsPerYear, numberOfDaysPerMonth = numberOfDaysPerMonth)

        underTest.processNodes(nodes)
        assertThat(underTest.getMonths()).hasSize(numberOfYears * numberOfMonthsPerYear)
    }

    @Test
    fun `test that a card is returned for every month`() {
        val numberOfYears = 1
        val numberOfDaysPerMonth = 1
        val numberOfMonthsPerYear = 6
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.processNodes(nodes)
        assertThat(underTest.getMonths()).hasSize(numberOfYears * numberOfMonthsPerYear)
    }

    @Test
    fun `test that only one year card is returned if all dates are in the same year`() {
        val numberOfYears = 1
        val numberOfMonthsPerYear = 6
        val numberOfDaysPerMonth = 1
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.processNodes(nodes)
        assertThat(underTest.getYears()).hasSize(numberOfYears)
    }

    @Test
    fun `test that a year card is returned for every year`() {
        val numberOfYears = 4
        val numberOfMonthsPerYear = 6
        val numberOfDaysPerMonth = 4
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.processNodes(nodes)
        assertThat(underTest.getYears()).hasSize(numberOfYears)
    }

    @Test
    fun `test missing preview on one day`() {
        val numberOfYears = 1
        val numberOfDaysPerMonth = 1
        val numberOfMonthsPerYear = 1
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.processNodes(nodes)
        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)

        val nodesWithoutPreview = underTest.getNodesWithoutPreview()
        assertThat(nodesWithoutPreview.size).isEqualTo(1)
        assertThat(nodesWithoutPreview.keys.first().base64Handle).isEqualTo(getHandleString(1,1,1))
    }

    @Test
    fun `test non missing preview on one day`() {
        whenever(fileUtilWrapper.getFileIfExists(any(), any())).thenReturn(File("Exists"))

        val numberOfYears = 1
        val numberOfDaysPerMonth = 2
        val numberOfMonthsPerYear = 1
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.processNodes(nodes)
        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)

        val nodesWithoutPreview = underTest.getNodesWithoutPreview()
        assertThat(nodesWithoutPreview.size).isEqualTo(0)
    }

    @Test
    fun `test missing preview on duplicate day is not in missing preview list`() {
        whenever(fileUtilWrapper.getFileIfExists(any(), argForWhich { contains("(1)") })).thenReturn(File("Exists"))
        whenever(fileUtilWrapper.getFileIfExists(any(), argForWhich { !contains("(1)") })).thenReturn(null)

        val numberOfYears = 1
        val numberOfDaysPerMonth = 1
        val numberOfMonthsPerYear = 1
        val numberOfDuplicateDays = 2
        val nodes = (1..numberOfDuplicateDays).map {
            getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth, it)
        }.flatten()

        nodes.forEach { println(it.base64Handle) }

        underTest.processNodes(nodes)
        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)

        val nodesWithoutPreview = underTest.getNodesWithoutPreview()
        assertThat(nodesWithoutPreview.size).isEqualTo(0)
    }

    @Test
    fun `test missing previews on multiple days`() {
        val numberOfYears = 2
        val numberOfDaysPerMonth = 3
        val numberOfMonthsPerYear = 4
        val nodes = getNodes(numberOfYears, numberOfMonthsPerYear, numberOfDaysPerMonth)

        underTest.processNodes(nodes)
        assertThat(underTest.getDays()).hasSize(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)

        val nodesWithoutPreview = underTest.getNodesWithoutPreview()
        assertThat(nodesWithoutPreview.size).isEqualTo(numberOfYears * numberOfMonthsPerYear * numberOfDaysPerMonth)
        assertThat(nodesWithoutPreview.keys.first().base64Handle).isEqualTo(getHandleString(1,1,1))
        assertThat(nodesWithoutPreview.keys.last().base64Handle).isEqualTo(getHandleString(3,4,2))
    }

    private fun getNodes(numberOfYears: Int, numberOfMonthsPerYear: Int, numberOfDaysPerMonth: Int, identifier: Int? = null): List<MegaNode> {
        val offset = OffsetDateTime.now().offset

        val nodes =
                (1..numberOfYears).map { year ->
                    (1..numberOfMonthsPerYear).map { month ->
                        (1..numberOfDaysPerMonth).map { day ->
                            LocalDateTime.of(year, month, day, 1, 1)
                        }
                    }.flatten()
                }.flatten()
                        .map { localDateTime ->
                            val day = localDateTime.dayOfMonth
                            val month = localDateTime.monthValue
                            val year = localDateTime.year
                            mock<MegaNode> {
                                on { base64Handle }.thenReturn(getHandleString(day, month, year) + appendIdentifier(identifier))
                                on { modificationTime }.thenReturn(localDateTime.toEpochSecond(offset))
                            }
                        }
        return nodes
    }

    private fun appendIdentifier(identifier: Int?) = identifier?.let { " ($it)" } ?: ""

    private fun getHandleString(day: Int, month: Int, year: Int) =
            "Day: $day Month:$month Year:$year"

}