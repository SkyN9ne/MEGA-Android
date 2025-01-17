package mega.privacy.android.app.presentation.extensions

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.annotation.StringRes
import timber.log.Timber
import java.util.Locale

/**
 * Get formatted string or default
 *
 * @param resId of the formatted sting
 * @param formatArgs Format arguments for the string
 */
fun Context.getFormattedStringOrDefault(@StringRes resId: Int, vararg formatArgs: Any?) =
    runCatching {
        getString(resId, *formatArgs)
    }.getOrElse { exception ->
        Timber.w(exception, "Error getting a translated and formatted string.")
        getEnglishResources().getString(resId, *formatArgs).also {
            Timber.i("Using the original English string: $it")
        }
    }

private fun Context.getEnglishResources(): Resources {
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(Locale(Locale.ENGLISH.language))
    return createConfigurationContext(configuration).resources
}

fun Context.getQuantityStringOrDefault(resId: Int, quantity: Int, vararg formatArgs: Any?): String {
    return runCatching {
        resources.getQuantityString(resId, quantity, *formatArgs)
    }.getOrElse { exception ->
        Timber.w(exception,
            "Error getting a translated and formatted string with quantity modifier.")
        getEnglishResources().getQuantityString(resId, quantity, *formatArgs).also {
            Timber.i("Using the original English string: $it")
        }
    }
}
