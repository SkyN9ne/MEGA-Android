package mega.privacy.android.domain.usecase

/**
 * Should keep file names
 *
 */
fun interface KeepFileNames {

    /**
     * Invoke
     *
     * @return whether file name should be kept
     */
    suspend operator fun invoke(): Boolean
}
