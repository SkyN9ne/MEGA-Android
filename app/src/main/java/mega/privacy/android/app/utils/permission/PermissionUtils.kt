package mega.privacy.android.app.utils.permission

import android.Manifest.permission.POST_NOTIFICATIONS
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import mega.privacy.android.app.R
import mega.privacy.android.app.main.ManagerActivity
import mega.privacy.android.app.presentation.permissions.NotificationsPermissionActivity
import mega.privacy.android.app.utils.Util
import timber.log.Timber

/**
 * Declare singleton PermissionUtils
 */
object PermissionUtils {
    const val TYPE_REQUIRE_PERMISSION = 0
    const val TYPE_GRANTED = 1
    const val TYPE_DENIED = 2
    const val TYPE_NEVER_ASK_AGAIN = 3

    /**
     * Request code for requesting notifications permission.
     */
    const val REQUEST_NOTIFICATIONS_PERMISSION = 6666

    /**
     * Checks all given permissions have been granted.
     *
     * @param grantResults results
     * @return returns true if all permissions have been granted.
     */
    @JvmStatic
    fun verifyPermissions(vararg grantResults: Int): Boolean {
        if (grantResults.isEmpty()) {
            return false
        }
        for (result in grantResults) {
            if (result != PermissionChecker.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    /**
     * Returns true if the Activity or Fragment has access to all given permissions.
     *
     * @param context     context
     * @param permissions permission list
     * @return returns true if the Activity or Fragment has access to all given permissions.
     */
    @JvmStatic
    fun hasSelfPermissions(context: Context, permissions: ArrayList<String>): Boolean {
        for (permission in permissions) {
            if (!hasSelfPermission(context, permission)) {
                return false
            }
        }
        return true
    }

    /**
     * Determine context has access to the given permission.
     *
     * @param context    context
     * @param permission permission
     * @return true if context has access to the given permission, false otherwise.
     */
    private fun hasSelfPermission(context: Context, permission: String): Boolean {
        return try {
            PermissionChecker.checkSelfPermission(
                context,
                permission
            ) == PermissionChecker.PERMISSION_GRANTED
        } catch (t: RuntimeException) {
            false
        }
    }

    /**
     * Get read permission regarding image based on sdk version
     *
     * @return read image permission based on sdk version
     */
    @JvmStatic
    fun getImagePermissionByVersion() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getReadImagePermission()
    } else {
        getReadExternalStoragePermission()
    }

    /**
     * Get read permission regarding audio based on sdk version
     *
     * @return read audio permission based on sdk version
     */
    @JvmStatic
    fun getAudioPermissionByVersion() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getReadAudioPermission()
    } else {
        getReadExternalStoragePermission()
    }

    /**
     * Get read permission regarding video based on sdk version
     *
     * @return read video permission based on sdk version
     */
    @JvmStatic
    fun getVideoPermissionByVersion() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getReadVideoPermission()
    } else {
        getReadExternalStoragePermission()
    }

    /**
     * Get READ_EXTERNAL_STORAGE
     *
     * @return READ_EXTERNAL_STORAGE
     */
    @JvmStatic
    fun getReadExternalStoragePermission() = READ_EXTERNAL_STORAGE

    /**
     * Get READ_MEDIA_AUDIO
     *
     * @return READ_MEDIA_AUDIO
     */
    @RequiresApi(33)
    private fun getReadAudioPermission() = READ_MEDIA_AUDIO

    /**
     * Get READ_MEDIA_IMAGES
     *
     * @return READ_MEDIA_IMAGES
     */
    @RequiresApi(33)
    private fun getReadImagePermission() = READ_MEDIA_IMAGES

    /**
     * Get READ_MEDIA_VIDEO
     *
     * @return READ_MEDIA_VIDEO
     */
    @RequiresApi(33)
    private fun getReadVideoPermission() = READ_MEDIA_VIDEO

    /**
     * Gets POST_NOTIFICATIONS
     *
     * @return POST_NOTIFICATIONS
     */
    @RequiresApi(33)
    private fun getNotificationsPermission() = POST_NOTIFICATIONS

    /**
     * Checks if should ask for notifications permission.
     *
     * @param activity Required Activity for the checks and launch intent.
     */
    @JvmStatic
    fun checkNotificationsPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && !hasPermissions(activity, getNotificationsPermission())
        ) {
            if (shouldShowRequestPermissionRationale(activity, getNotificationsPermission())) {
                activity.startActivity(Intent(activity,
                    NotificationsPermissionActivity::class.java))
            } else {
                requestPermission(activity,
                    REQUEST_NOTIFICATIONS_PERMISSION,
                    getNotificationsPermission())
            }
        }
    }

    /**
     * Checks given permissions are needed to show rationale.
     *
     * @param activity    activity
     * @param permissions permission list
     * @return returns true if one of the permission is needed to show rationale.
     */
    @JvmStatic
    fun shouldShowRequestPermissionRationale(
        activity: Activity?,
        permissions: List<String>,
    ): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity!!, permission)) {
                return true
            }
        }
        return false
    }

    /**
     * Checks given permissions are needed to show rationale.
     *
     * @param fragment    fragment
     * @param permissions permission list
     * @return returns true if one of the permission is needed to show rationale.
     */
    @JvmStatic
    fun shouldShowRequestPermissionRationale(
        fragment: Fragment,
        permissions: List<String>,
    ): Boolean {
        for (permission in permissions) {
            if (fragment.shouldShowRequestPermissionRationale(permission)) {
                return true
            }
        }
        return false
    }

    /**
     * Check if the user ticket 'Don't ask again' and deny a permission request.
     * In this case, the system request permission dialog can no longer show up.
     *
     * @param activity the Context.
     * @param permission which permission to check.
     * @return false if the user ticket 'Don't ask again' and deny, otherwise true.
     */
    @JvmStatic
    fun shouldShowRequestPermissionRationale(
        @NonNull activity: Activity,
        @NonNull permission: String,
    ): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Provide an OnClickListener for snackbar's action.
     *
     * @param context Context.
     * @return an OnClickListener, which leads to the APP info page, in where, users can grant MEGA permissions.
     */
    @JvmStatic
    fun toAppInfo(@NonNull context: Context): View.OnClickListener {
        return View.OnClickListener {
            val intent = Intent()
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            val uri = Uri.fromParts("package", context.packageName, null)
            intent.data = uri
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                if (context is ManagerActivity) {
                    // in case few devices cannot handle 'ACTION_APPLICATION_DETAILS_SETTINGS' action.
                    Util.showSnackbar(
                        context,
                        context.getString(R.string.on_permanently_denied)
                    )
                } else {
                    Timber.e(e, "Exception opening device settings")
                }
            }
        }
    }

    /**
     * Check permissions whether are granted
     * @param context Context
     * @param permissions one or more permission strings
     * @return whether permissions has been granted
     */
    @JvmStatic
    fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
        if (context != null) {
            for (permission in permissions) {
                // In Android 11+ WRITE_EXTERNAL_STORAGE doesn't grant any addition access so can assume it has been granted
                if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.R || permission != WRITE_EXTERNAL_STORAGE) &&
                    ContextCompat.checkSelfPermission(
                        context,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Ask permissions
     * @param activity The activity
     * @param requestCode request code of permission asking
     * @param permissions requested permissions
     */
    @JvmStatic
    fun requestPermission(
        @NonNull activity: Activity,
        @NonNull requestCode: Int,
        @NonNull vararg permissions: String?,
    ) {
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            requestCode
        )
    }

    /**
     * Callback function for granting permissions for sub class
     *
     * @param permissions permission list
     */
    @JvmStatic
    fun onRequiresPermission(
        permissions: ArrayList<String>,
        permissionCallbacks: PermissionCallbacks,
    ) {
        permissionCallbacks.onPermissionsCallback(TYPE_REQUIRE_PERMISSION, permissions)
    }

    /**
     * Process when the user denies the permissions
     *
     * @param permissions permission list
     */
    @JvmStatic
    fun onPermissionDenied(
        permissions: ArrayList<String>,
        permissionCallbacks: PermissionCallbacks,
    ) {
        permissionCallbacks.onPermissionsCallback(TYPE_DENIED, permissions)
    }

    /**
     * Callback function that allow for continuation or cancellation of a permission request..
     *
     * @param request allow for continuation or cancellation of a permission request.
     */
    @JvmStatic
    fun onShowRationale(request: PermissionRequest) {
        request.proceed()
    }

    /**
     * Callback function that will be called when the user denies the permissions and tickets "Never Ask Again" after calls requestPermissions()
     *
     * @param permissions permission list
     */
    @JvmStatic
    fun onNeverAskAgain(permissions: ArrayList<String>, permissionCallbacks: PermissionCallbacks) {
        permissionCallbacks.onPermissionsCallback(TYPE_NEVER_ASK_AGAIN, permissions)
    }

    /**
     * Callback interface to receive the results of permissions request
     */
    interface PermissionCallbacks {
        fun onPermissionsCallback(requestType: Int, perms: ArrayList<String>)
    }
}