package mega.privacy.android.app.presentation.photos.model

import mega.privacy.android.app.usecase.MegaException

/**
 * The album list load state
 */
sealed interface AlbumsLoadState {
    /**
     * Get album list success
     * @param albums album list
     */
    data class Success(val albums: List<AlbumCoverItem>) : AlbumsLoadState

    /**
     * Loading state
     */
    object Loading : AlbumsLoadState

    /**
     * album list is empty
     */
    data class Empty(val albums: List<AlbumCoverItem>) : AlbumsLoadState

    /**
     * Get album list error
     * @param exception MegaException
     */
    data class Error(val exception: MegaException) : AlbumsLoadState
}