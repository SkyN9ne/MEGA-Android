package mega.privacy.android.data.facade

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.account.AccountDetail
import mega.privacy.android.domain.entity.billing.MegaPurchase
import nz.mega.sdk.MegaRequest

/**
 * Account info wrapper to create an interface to Account information functionality
 *
 */
interface AccountInfoWrapper {
    /**
     * Storage capacity used as a formatted string
     */
    val storageCapacityUsedAsFormattedString: String

    /**
     * Account type id
     *
     * Options:
     *
     * Default/Invalid = -1
     *
     * MegaAccountDetails.ACCOUNT_TYPE_FREE = 0
     * MegaAccountDetails.ACCOUNT_TYPE_PROI = 1
     * MegaAccountDetails.ACCOUNT_TYPE_PROII = 2
     * MegaAccountDetails.ACCOUNT_TYPE_PROIII = 3
     * MegaAccountDetails.ACCOUNT_TYPE_LITE = 4
     * MegaAccountDetails.ACCOUNT_TYPE_BUSINESS = 100
     */
    val accountTypeId: Int

    /**
     * Account type string
     */
    val accountTypeString: String

    /**
     * Handle account detail
     * Legacy support, it will remove after refactor done
     *
     * @param request
     */
    suspend fun handleAccountDetail(request: MegaRequest)

    /**
     * Handle account detail
     *
     *
     * @param newDetail
     */
    suspend fun handleAccountDetail(newDetail: AccountDetail)

    /**
     * Update active subscription
     *
     */
    fun updateActiveSubscription(purchase: MegaPurchase?)

    /**
     * Subscription payment method
     */
    val subscriptionMethodId: Int

    /**
     * Monitor account detail
     *
     */
    fun monitorAccountDetail(): Flow<AccountDetail>
}
