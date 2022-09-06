package mega.privacy.android.app.listeners

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.jobservices.CameraUploadsService
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.main.megachat.NodeAttachmentHistoryActivity
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.JobUtil
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import nz.mega.sdk.MegaRequestListenerInterface
import timber.log.Timber

/**
 * Listener for create folder response handling
 *
 * @param context: Context
 * @param extraAction : ExtraAction
 */
class CreateFolderListener @JvmOverloads constructor(
    private val context: Context,
    private var extraAction: ExtraAction = ExtraAction.NONE,
) : MegaRequestListenerInterface {

    /**
     * Enum to identify ExtraAction
     */
    enum class ExtraAction {
        /**
         * NONE
         */
        NONE,

        /**
         * MY_CHAT_FILES
         */
        MY_CHAT_FILES,

        /**
         * MY_CHAT_FILES
         */
        INIT_CAMERA_UPLOAD
    }

    /**
     * Callback function for onRequestStart
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestStart(api: MegaApiJava?, request: MegaRequest?) {
        // Do nothing
    }

    /**
     * Callback function for onRequestUpdate
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     */
    override fun onRequestUpdate(api: MegaApiJava?, request: MegaRequest?) {
        // Do nothing
    }

    /**
     * Callback function for onRequestFinish
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestFinish(api: MegaApiJava, request: MegaRequest, e: MegaError) {
        if (request.type == MegaRequest.TYPE_CREATE_FOLDER) {
            val handle = request.nodeHandle
            val node = api.getNodeByHandle(request.nodeHandle)

            if (extraAction == ExtraAction.INIT_CAMERA_UPLOAD) {
                if (request.name == context.getString(R.string.section_photo_sync)) {
                    CameraUploadsService.isCreatingPrimary = false
                    //set primary only
                    setCameraUploadFolder(megaError = e,
                        nodeHandle = handle,
                        isPrimary = true,
                        megaApi = api)
                } else if (request.name == context.getString(R.string.section_secondary_media_uploads)) {
                    CameraUploadsService.isCreatingSecondary = false;
                    //set secondary only
                    setCameraUploadFolder(megaError = e,
                        nodeHandle = handle,
                        isPrimary = false,
                        megaApi = api)
                }
            }

            when (context) {
                is FileExplorerActivity -> {
                    if (e.errorCode == MegaError.API_OK) {
                        if (extraAction == ExtraAction.MY_CHAT_FILES) {
                            context.setMyChatFilesFolder(node)
                            api.setMyChatFilesFolder(handle, SetAttrUserListener(context))
                            context.checkIfFilesExistsInMEGA()
                        } else {
                            context.finishCreateFolder(true, handle)
                        }
                    } else {
                        if (extraAction == ExtraAction.MY_CHAT_FILES) {
                            context.showSnackbar(context.getString(R.string.general_text_error))
                        } else {
                            context.finishCreateFolder(false, handle)
                            context.showSnackbar(context.getString(R.string.error_creating_folder,
                                request.name))
                        }
                    }
                }

                is ChatActivity -> {
                    if (e.errorCode == MegaError.API_OK) {
                        api.setMyChatFilesFolder(handle, SetAttrUserListener(context))
                        context.setMyChatFilesFolder(node)
                        if (context.isForwardingFromNC) {
                            context.handleStoredData()
                        } else {
                            context.proceedWithAction()
                        }
                    } else {
                        context.showSnackbar(Constants.SNACKBAR_TYPE,
                            context.getString(R.string.general_text_error),
                            -1)
                    }
                }

                is NodeAttachmentHistoryActivity -> {
                    if (e.errorCode == MegaError.API_OK) {
                        api.setMyChatFilesFolder(handle, SetAttrUserListener(context))
                        context.setMyChatFilesFolder(node)
                        context.handleStoredData()
                    } else {
                        context.showSnackbar(Constants.SNACKBAR_TYPE,
                            context.getString(R.string.general_text_error))
                    }
                }

                is CameraUploadsService -> {
                    context.onCreateFolder(e.errorCode == MegaError.API_OK)
                }
            }

            if (e.errorCode != MegaError.API_OK) {
                Timber.e("Error creating folder: %s", e.errorString)
            }
        }
    }

    /**
     * Callback function for onRequestTemporaryError
     *
     * @param api : MegaApiJava
     * @param request : MegaRequest
     * @param e: MegaError
     */
    override fun onRequestTemporaryError(api: MegaApiJava?, request: MegaRequest?, e: MegaError?) {
        // Do nothing
    }

    /**
     * Function to set camera upload folder based on response
     *
     * @param megaError : MegaError
     * @param nodeHandle: Long
     * @param isPrimary: Boolean
     * @param megaApi: MegaApiJava
     */
    private fun setCameraUploadFolder(
        megaError: MegaError,
        nodeHandle: Long,
        isPrimary: Boolean,
        megaApi: MegaApiJava,
    ) {
        if (megaError.errorCode == MegaError.API_OK) {
            if (isPrimary) {
                Timber.d("Set CU primary attribute on create folder: %s", nodeHandle)
                megaApi.setCameraUploadsFolders(nodeHandle,
                    MegaApiJava.INVALID_HANDLE,
                    SetAttrUserListener(context))
            } else {
                Timber.d("Set CU secondary attribute on create folder: %s", nodeHandle)
                megaApi.setCameraUploadsFolders(MegaApiJava.INVALID_HANDLE,
                    nodeHandle,
                    SetAttrUserListener(context))
            }
        } else {
            val folderName = if (isPrimary) {
                "CU"
            } else {
                "MU"
            }
            Timber.w("Create %s folder failed, error code: %d, %s",
                folderName,
                megaError.errorCode,
                megaError.errorString)
            JobUtil.fireStopCameraUploadJob(context)
        }
    }
}