package test.mega.privacy.android.app.presentation.manager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.jraska.livedata.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.usecase.GetInboxNode
import mega.privacy.android.app.domain.usecase.GetPrimarySyncHandle
import mega.privacy.android.app.domain.usecase.GetSecondarySyncHandle
import mega.privacy.android.app.domain.usecase.MonitorGlobalUpdates
import mega.privacy.android.app.presentation.manager.ManagerViewModel
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.manager.model.TransfersTab
import mega.privacy.android.data.model.GlobalUpdate
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import mega.privacy.android.domain.usecase.CheckCameraUpload
import mega.privacy.android.domain.usecase.GetCloudSortOrder
import mega.privacy.android.domain.usecase.GetNumUnreadUserAlerts
import mega.privacy.android.domain.usecase.HasInboxChildren
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorContactRequestUpdates
import mega.privacy.android.domain.usecase.MonitorMyAvatarFile
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.SendStatisticsMediaDiscovery
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.presentation.shares.FakeMonitorUpdates
import java.util.concurrent.TimeUnit

@ExperimentalCoroutinesApi
class ManagerViewModelTest {
    private lateinit var underTest: ManagerViewModel

    private val monitorGlobalUpdates = mock<MonitorGlobalUpdates>()
    private val monitorNodeUpdates = FakeMonitorUpdates()
    private val getNumUnreadUserAlerts = mock<GetNumUnreadUserAlerts>()
    private val hasInboxChildren = mock<HasInboxChildren>()
    private val monitorContactRequestUpdates = mock<MonitorContactRequestUpdates>()
    private val sendStatisticsMediaDiscovery = mock<SendStatisticsMediaDiscovery>()
    private val savedStateHandle = SavedStateHandle(mapOf())
    private val monitorMyAvatarFile = mock<MonitorMyAvatarFile>()
    private val getInboxNode = mock<GetInboxNode>()
    private val monitorStorageState = mock<MonitorStorageStateEvent>()
    private val getPrimarySyncHandle = mock<GetPrimarySyncHandle>()
    private val getSecondarySyncHandle = mock<GetSecondarySyncHandle>()
    private val checkCameraUpload = mock<CheckCameraUpload>()
    private val getCloudSortOrder = mock<GetCloudSortOrder>()
    private val monitorConnectivity = mock<MonitorConnectivity>()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }


    /**
     * Initialize the view model under test
     */
    private fun setUnderTest() {
        underTest = ManagerViewModel(
            monitorNodeUpdates = monitorNodeUpdates,
            monitorGlobalUpdates = monitorGlobalUpdates,
            monitorContactRequestUpdates = monitorContactRequestUpdates,
            getNumUnreadUserAlerts = getNumUnreadUserAlerts,
            hasInboxChildren = hasInboxChildren,
            sendStatisticsMediaDiscovery = sendStatisticsMediaDiscovery,
            savedStateHandle = savedStateHandle,
            monitorMyAvatarFile = monitorMyAvatarFile,
            ioDispatcher = StandardTestDispatcher(),
            getInboxNode = getInboxNode,
            monitorStorageStateEvent = monitorStorageState,
            getPrimarySyncHandle = getPrimarySyncHandle,
            getSecondarySyncHandle = getSecondarySyncHandle,
            checkCameraUpload = checkCameraUpload,
            getCloudSortOrder = getCloudSortOrder,
            monitorConnectivity = monitorConnectivity,
            broadcastUploadPauseState = mock(),
            getExtendedAccountDetail = mock(),
            getPricing = mock(),
            getFullAccountInfo = mock(),
            getActiveSubscription = mock(),
        )
    }

    /**
     * Simulate a repository emission and setup the [ManagerViewModel]
     *
     * @param updates the values to emit from the repository
     * @param after a lambda function to call after setting up the viewModel
     */
    @Suppress("DEPRECATION")
    private fun triggerRepositoryUpdate(updates: List<GlobalUpdate>, after: () -> Unit) {
        whenever(monitorGlobalUpdates()).thenReturn(updates.asFlow())
        executeTest(after)
    }

    private fun executeTest(after: () -> Unit) {
        setUnderTest()
        after()
    }

    @Test
    fun `test that initial state is returned`() = runTest {
        setUnderTest()
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.isFirstNavigationLevel).isTrue()
            assertThat(initial.sharesTab).isEqualTo(SharesTab.INCOMING_TAB)
            assertThat(initial.transfersTab).isEqualTo(TransfersTab.NONE)
            assertThat(initial.isFirstLogin).isFalse()
            assertThat(initial.shouldSendCameraBroadcastEvent).isFalse()
            assertThat(initial.shouldStopCameraUpload).isFalse()
            assertThat(initial.nodeUpdateReceived).isFalse()
        }
    }

    @Test
    fun `test that is first navigation level is updated if new value provided`() = runTest {
        setUnderTest()

        underTest.state.map { it.isFirstNavigationLevel }.distinctUntilChanged()
            .test {
                val newValue = false
                assertThat(awaitItem()).isEqualTo(true)
                underTest.setIsFirstNavigationLevel(newValue)
                assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that shares tab is updated if new value provided`() = runTest {
        setUnderTest()

        underTest.state.map { it.sharesTab }.distinctUntilChanged()
            .test {
                val newValue = SharesTab.OUTGOING_TAB
                assertThat(awaitItem()).isEqualTo(SharesTab.INCOMING_TAB)
                underTest.setSharesTab(newValue)
                assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that transfers tab is updated if new value provided`() = runTest {
        setUnderTest()

        underTest.state.map { it.transfersTab }.distinctUntilChanged()
            .test {
                val newValue = TransfersTab.PENDING_TAB
                assertThat(awaitItem()).isEqualTo(TransfersTab.NONE)
                underTest.setTransfersTab(newValue)
                assertThat(awaitItem()).isEqualTo(newValue)
            }
    }

    @Test
    fun `test that user updates live data is not set when no updates triggered from use case`() =
        runTest {
            setUnderTest()

            underTest.updateUsers.test().assertNoValue()
        }

    @Test
    fun `test that user alert updates live data is not set when no updates triggered from use case`() =
        runTest {
            setUnderTest()

            underTest.updateUserAlerts.test().assertNoValue()
        }

    @Test
    fun `test that contact request updates live data is not set when no updates triggered from use case`() =
        runTest {
            setUnderTest()

            underTest.updateContactsRequests.test().assertNoValue()
        }

    @Test
    fun `test that user updates live data is set when user updates triggered from use case`() =
        runTest {
            triggerRepositoryUpdate(listOf(GlobalUpdate.OnUsersUpdate(arrayListOf(mock())))) {

                runCatching {
                    underTest.updateUsers.test().awaitValue(50, TimeUnit.MILLISECONDS)
                }.onSuccess { result ->
                    result.assertValue { it.getContentIfNotHandled()?.size == 1 }
                }
            }
        }

    @Test
    fun `test that user updates live data is not set when user updates triggered from use case with null`() =
        runTest {
            triggerRepositoryUpdate(
                listOf(
                    GlobalUpdate.OnUsersUpdate(null),
                )
            ) {
                underTest.updateUsers.test().assertNoValue()
            }
        }

    @Test
    fun `test that user alert updates live data is set when user alert updates triggered from use case`() =
        runTest {
            triggerRepositoryUpdate(listOf(GlobalUpdate.OnUserAlertsUpdate(arrayListOf(mock())))) {

                runCatching {
                    underTest.updateUserAlerts.test().awaitValue(50, TimeUnit.MILLISECONDS)
                }.onSuccess { result ->
                    result.assertValue { it.getContentIfNotHandled()?.size == 1 }
                }
            }
        }

    @Test
    fun `test that user alert updates live data is not set when user alert updates triggered from use case with null`() =
        runTest {
            triggerRepositoryUpdate(
                listOf(
                    GlobalUpdate.OnUserAlertsUpdate(null),
                )
            ) {
                underTest.updateUserAlerts.test().assertNoValue()
            }
        }

    @Test
    fun `test that contact request updates live data is set when contact request updates triggered from use case`() =
        runTest {
            whenever(monitorContactRequestUpdates()).thenReturn(flowOf(listOf(
                ContactRequest(
                    handle = 1L,
                    sourceEmail = "",
                    sourceMessage = null,
                    targetEmail = "",
                    creationTime = 1L,
                    modificationTime = 1L,
                    status = ContactRequestStatus.Unresolved,
                    isOutgoing = false,
                    isAutoAccepted = false))))
            executeTest {

                runCatching {
                    underTest.updateContactsRequests.test().awaitValue(50, TimeUnit.MILLISECONDS)
                }.onSuccess { result ->
                    result.assertValue { it.getContentIfNotHandled()?.size == 1 }
                }
            }
        }

    @Test
    fun `test that contact request updates live data is not set when contact request updates triggered from use case with null`() =
        runTest {
            executeTest {
                underTest.updateContactsRequests.test().assertNoValue()
            }
        }

    @Test
    fun `test that saved state values are returned`() = runTest {
        setUnderTest()

        savedStateHandle[underTest.isFirstLoginKey] = true

        underTest.state.filter {
            it.isFirstLogin
        }.test(200) {
            val latest = awaitItem()
            assertThat(latest.isFirstLogin).isTrue()
        }
    }

    @Test
    fun `test that is first login is updated if new boolean is provided`() = runTest {
        setUnderTest()
        underTest.state.map { it.isFirstLogin }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isFalse()
                underTest.setIsFirstLogin(true)
                assertThat(awaitItem()).isTrue()
            }
    }

    @Test
    fun `test that get order returns cloud sort order`() = runTest {
        setUnderTest()
        val expected = SortOrder.ORDER_MODIFICATION_DESC
        whenever(getCloudSortOrder()).thenReturn(expected)
        assertThat(underTest.getOrder()).isEqualTo(expected)
    }

    @Test
    fun `test that updateToolbarTitle is true when a node update occurs`() = runTest {
        setUnderTest()

        underTest.state.map { it.nodeUpdateReceived }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            monitorNodeUpdates.emit(listOf(mock()))
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that updateToolbarTitle is set when calling setUpdateToolbar`() = runTest {
        setUnderTest()

        underTest.state.map { it.nodeUpdateReceived }.distinctUntilChanged().test {
            assertThat(awaitItem()).isFalse()
            monitorNodeUpdates.emit(listOf(mock()))
            assertThat(awaitItem()).isTrue()
            underTest.nodeUpdateHandled()
            assertThat(awaitItem()).isFalse()
        }
    }
}
