package mega.privacy.android.domain.usecase

/**
 * Get video quality setting
 *
 */
fun interface GetVideoQuality {

    /**
     * Invoke
     *
     * @return video quality setting
     */
    operator fun invoke(): Int
}