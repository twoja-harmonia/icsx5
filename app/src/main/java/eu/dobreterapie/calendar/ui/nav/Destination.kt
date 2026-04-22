/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package eu.dobreterapie.calendar.ui.nav

import androidx.annotation.ColorInt
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destination : NavKey {
    @Serializable
    object Onboarding : Destination

    @Serializable
    object Welcome : Destination

    @Serializable
    object QrScanner : Destination

    @Serializable
    object SubscriptionList : Destination

    @Serializable
    object Info : Destination

    @Serializable
    data class AddSubscription(
        val title: String? = null,
        @param:ColorInt val color: Int? = null,
        val url: String? = null,
    ): Destination

    @Serializable
    data class EditSubscription(
        val subscriptionId: Long
    ): Destination
}
