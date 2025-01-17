package mega.privacy.android.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.VideoQuality
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultEnablePhotosCameraUploadTest {
    private lateinit var underTest: EnablePhotosCameraUpload

    private val settingsRepository = mock<SettingsRepository>()

    @Before
    fun setUp() {
        underTest = DefaultEnablePhotosCameraUpload(settingsRepository = settingsRepository)
    }

    @Test
    fun `test that initialisation functions are called in order`() = runTest {
        val expectedPath = "path"
        val expectedSyncVideo = true
        val expectedEnableCellularSync = true
        val expectedVideoQuality = VideoQuality.HIGH
        val expectedConversionChargingOnSize = 12

        underTest(
            path = expectedPath,
            syncVideo = expectedSyncVideo,
            enableCellularSync = expectedEnableCellularSync,
            videoQuality = expectedVideoQuality,
            conversionChargingOnSize = expectedConversionChargingOnSize,
        )

        val inOrder = inOrder(settingsRepository)

        inOrder.verify(settingsRepository).setCameraUploadLocalPath(expectedPath)
        inOrder.verify(settingsRepository).setCamSyncWifi(!expectedEnableCellularSync)
        inOrder.verify(settingsRepository).setCameraUploadFileType(expectedSyncVideo)
        inOrder.verify(settingsRepository).setCameraFolderExternalSDCard(false)
        inOrder.verify(settingsRepository).setCameraUploadVideoQuality(expectedVideoQuality)
        inOrder.verify(settingsRepository).setConversionOnCharging(true)
        inOrder.verify(settingsRepository).setChargingOnSize(expectedConversionChargingOnSize)
        inOrder.verify(settingsRepository).setEnableCameraUpload(true)

        inOrder.verifyNoMoreInteractions()

    }
}