package mega.privacy.android.app.initializer

import android.content.Context
import androidx.startup.Initializer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.BuildConfig
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.InitialiseLogging

/**
 * Logger initializer
 *
 */
class LoggerInitializer : Initializer<Unit> {
    /**
     * Logger initializer entry point
     *
     */
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface LoggerInitializerEntryPoint {
        /**
         * Initialise logging
         *
         */
        fun initialiseLogging(): InitialiseLogging

        /**
         * App scope
         *
         */
        @ApplicationScope
        fun appScope(): CoroutineScope
    }

    /**
     * Create
     *
     */
    override fun create(context: Context) {
        val entryPoint =
            EntryPointAccessors.fromApplication(context, LoggerInitializerEntryPoint::class.java)

        entryPoint.appScope().launch {
            entryPoint.initialiseLogging().invoke(BuildConfig.DEBUG)
        }
    }

    /**
     * Dependencies
     *
     */
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}