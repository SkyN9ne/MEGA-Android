package mega.privacy.android.app.audioplayer.playlist

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import java.io.File
import java.util.*

data class PlaylistItem(
    val nodeHandle: Long,
    val nodeName: String,
    val thumbnail: File?,
    val index: Int,
    val type: Int,
) {
    fun finalizeThumbnailAndType(type: Int): PlaylistItem {
        return PlaylistItem(
            nodeHandle, nodeName,
            if (thumbnail?.exists() == true) thumbnail else null,
            index, type
        )
    }

    fun updateNodeName(newName: String) = PlaylistItem(nodeHandle, newName, thumbnail, index, type)

    companion object {
        const val TYPE_PREVIOUS = 1
        const val TYPE_PREVIOUS_HEADER = 2
        const val TYPE_PLAYING = 3
        const val TYPE_PLAYING_HEADER = 4
        const val TYPE_NEXT = 5
        const val TYPE_NEXT_HEADER = 6

        // We can't use the same handle (INVALID_HANDLE) for multiple header items,
        // which will cause display issue when PlaylistItemDiffCallback use
        // handle for areItemsTheSame.
        // RandomUUID() can ensure non-repetitive values in practical purpose.
        private val previousHeaderHandle = UUID.randomUUID().leastSignificantBits
        private val playingHeaderHandle = UUID.randomUUID().leastSignificantBits
        private val nextHeaderHandle = UUID.randomUUID().leastSignificantBits

        fun headerItem(context: Context, type: Int, paused: Boolean = false): PlaylistItem {
            val name = context.getString(
                when (type) {
                    TYPE_PREVIOUS_HEADER -> R.string.general_previous
                    TYPE_NEXT_HEADER -> R.string.general_next
                    else -> {
                        if (paused) {
                            R.string.audio_player_now_playing_paused
                        } else {
                            R.string.audio_player_now_playing
                        }
                    }
                }
            )

            val handle = when (type) {
                TYPE_PREVIOUS_HEADER -> previousHeaderHandle
                TYPE_NEXT_HEADER -> nextHeaderHandle
                else -> playingHeaderHandle
            }

            return PlaylistItem(handle, name, null, INVALID_VALUE, type)
        }
    }
}
