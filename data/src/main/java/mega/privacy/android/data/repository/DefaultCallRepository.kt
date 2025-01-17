package mega.privacy.android.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.CallRepository
import mega.privacy.android.domain.repository.ChatRepository
import timber.log.Timber
import javax.inject.Inject

internal class DefaultCallRepository @Inject constructor(
    private val chatRepository: ChatRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CallRepository {

    override suspend fun startCall(
        chatId: Long,
        video: Boolean,
        audio: Boolean,
    ): ChatCall? =
        withContext(ioDispatcher) {
            runCatching {
                chatRepository.startChatCall(chatId = chatId,
                    enabledVideo = video,
                    enabledAudio = audio)
            }.fold(
                onSuccess = { request ->
                    request.chatHandle?.let { id ->
                        return@withContext chatRepository.getChatCall(id)
                    }
                    return@withContext null
                },
                onFailure = { exception ->
                    Timber.e(exception)
                    return@withContext null
                }
            )
        }
}