/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package eu.dobreterapie.calendar.model

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.dobreterapie.calendar.Constants
import eu.dobreterapie.calendar.R
import eu.dobreterapie.calendar.SyncWorker
import eu.dobreterapie.calendar.db.AppDatabase
import eu.dobreterapie.calendar.db.dao.SubscriptionsDao
import eu.dobreterapie.calendar.db.entity.Credential
import eu.dobreterapie.calendar.db.entity.Subscription
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel(assistedFactory = EditSubscriptionModel.EditSubscriptionModelFactory::class)
class EditSubscriptionModel @AssistedInject constructor(
    private val db: AppDatabase,
    @param:ApplicationContext val context: Context,
    @Assisted private val subscriptionId: Long,
    val subscriptionSettingsUseCase: SubscriptionSettingsUseCase
): ViewModel() {

    @AssistedFactory
    interface EditSubscriptionModelFactory {
        fun create(subscriptionId: Long): EditSubscriptionModel
    }

    private var initialSubscription: Subscription? = null
    private var initialCredential: Credential? = null
    private val initialRequiresAuth: Boolean get() = initialCredential != null

    /**
     * Whether user input is error free
     */
    val inputValid: Boolean
        get() = with(subscriptionSettingsUseCase) {
            val title = uiState.title
            val requiresAuth = uiState.requiresAuth
            val username = uiState.username
            val password = uiState.password

            val titleOK = !title.isNullOrBlank()
            val authOK = if (requiresAuth)
                !username.isNullOrBlank() && !password.isNullOrBlank()
            else
                true
            titleOK && authOK
        }

    /**
     * Whether there are unsaved user changes
     */
    val modelsDirty: Boolean
        get() = with(subscriptionSettingsUseCase) {
            val requiresAuth = uiState.requiresAuth

            val credentialsDirty = initialRequiresAuth != requiresAuth || initialCredential?.let {
                !equalsCredential(it)
            } ?: false
            val subscriptionsDirty = initialSubscription?.let {
                !equalsSubscription(it)
            } ?: false

            credentialsDirty || subscriptionsDirty
        }

    var subscriptionWithCredential: SubscriptionsDao.SubscriptionWithCredential? = null
        private set

    init {
        viewModelScope.launch {
            db.subscriptionsDao().getWithCredentialsById(subscriptionId)?.let {
                onSubscriptionLoaded(it)
            }
        }
    }

    /**
     * Initialise view models and remember their initial state
     */
    private fun onSubscriptionLoaded(subscriptionWithCredential: SubscriptionsDao.SubscriptionWithCredential) {
        val subscription = subscriptionWithCredential.subscription
        val credential = subscriptionWithCredential.credential

        // Save the initial state, before updating the UI, so the state is persisted
        initialSubscription = subscription
        initialCredential = credential
        this.subscriptionWithCredential = subscriptionWithCredential

        subscriptionSettingsUseCase.update(subscription, credential)
    }

    private suspend fun showMessage(@StringRes message: Int) = withContext(Dispatchers.Main) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Updates the loaded subscription from the data provided by the view models.
     */
    fun updateSubscription() = with(subscriptionSettingsUseCase.uiState) {
        viewModelScope.launch(Dispatchers.IO) {
            subscriptionWithCredential?.let { (subscription) ->
                val newSubscription = subscription.copy(
                    displayName = title ?: subscription.displayName,
                    color = color,
                    customUserAgent = customUserAgent,
                    defaultAlarmMinutes = defaultAlarmMinutes,
                    defaultAllDayAlarmMinutes = defaultAllDayAlarmMinutes,
                    ignoreEmbeddedAlerts = ignoreAlerts,
                    ignoreDescription = ignoreDescription
                )
                db.subscriptionsDao().update(newSubscription)

                if (requiresAuth) {
                    if (username != null && password != null)
                        db.credentialsDao().upsert(Credential(subscriptionId, username, password))
                } else
                    db.credentialsDao().removeBySubscriptionId(subscriptionId)

                // notify UI about success
                showMessage(R.string.edit_calendar_saved)

                // sync the subscription to reflect the changes in the calendar provider
                SyncWorker.run(context, forceResync = true)
            } ?: Log.w(Constants.TAG, "There's no subscription to update")
        }
    }

    /**
     * Removes the loaded subscription.
     */
    fun removeSubscription() = viewModelScope.launch(Dispatchers.IO) {
        subscriptionWithCredential?.let { (subscription) ->
            db.subscriptionsDao().delete(subscription)

            // sync the subscription to reflect the changes in the calendar provider
            SyncWorker.run(context)

            // notify UI about success
            showMessage(R.string.edit_calendar_deleted)
        } ?: Log.w(Constants.TAG, "There's no subscription to remove")
    }

}