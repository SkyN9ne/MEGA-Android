package mega.privacy.android.app.di.calls

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.presentation.calls.facade.OpenCallWrapper
import mega.privacy.android.app.utils.OpenCallHelper

@Module
@InstallIn(SingletonComponent::class)
abstract class OpenCallModule {

    @Binds
    abstract fun bindOpenCallWrapper(useCase: OpenCallHelper): OpenCallWrapper
}