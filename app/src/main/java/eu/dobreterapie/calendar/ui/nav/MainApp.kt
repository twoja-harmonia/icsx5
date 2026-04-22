/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package eu.dobreterapie.calendar.ui.nav

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import eu.dobreterapie.calendar.MainActivity.Companion.EXTRA_ERROR_MESSAGE
import eu.dobreterapie.calendar.MainActivity.Companion.EXTRA_REQUEST_CALENDAR_PERMISSION
import eu.dobreterapie.calendar.MainActivity.Companion.EXTRA_THROWABLE
import eu.dobreterapie.calendar.R
import eu.dobreterapie.calendar.service.ComposableStartupService
import eu.dobreterapie.calendar.ui.WinterEasterEgg
import eu.dobreterapie.calendar.ui.partials.AlertDialog
import eu.dobreterapie.calendar.ui.screen.AddSubscriptionScreen
import eu.dobreterapie.calendar.ui.screen.EditSubscriptionScreen
import eu.dobreterapie.calendar.ui.screen.InfoScreen
import eu.dobreterapie.calendar.ui.screen.SubscriptionsScreen
import eu.dobreterapie.calendar.ui.screen.WelcomeScreen
import eu.dobreterapie.calendar.ui.screen.OnboardingScreen
import eu.dobreterapie.calendar.ui.screen.QrScannerScreen
import eu.dobreterapie.calendar.ui.screen.isOnboardingCompleted
import kotlinx.coroutines.launch
import java.util.ServiceLoader

/**
 * Computes the correct initial destination from some intent:
 * - If [AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE] is present -> [Destination.AddSubscription]
 * - Otherwise: [Destination.Welcome]
 */
private fun calculateInitialDestination(intent: Intent, context: Context): Destination {
    val extras = intent.extras ?: Bundle.EMPTY
    val text = extras.getString(Intent.EXTRA_TEXT)
        ?.trim()
        ?.takeUnless { it.isEmpty() }
    val stream = BundleCompat.getParcelable(extras, Intent.EXTRA_STREAM, Uri::class.java)
        ?.toString()
    val data = intent.dataString

    return if (extras.containsKey(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)) {
        Destination.AddSubscription()
    } else if (text != null || stream != null || data != null) {
        Destination.AddSubscription(
            title = extras.getString("title"),
            color = extras.takeIf { it.containsKey("color") }?.getInt("color", -1),
            url = text ?: stream ?: data
        )
    } else {
        if (isOnboardingCompleted(context)) Destination.Welcome else Destination.Onboarding
    }
}

@Composable
fun MainApp(
    savedInstanceState: Bundle?,
    intent: Intent,
    onFinish: () -> Unit,
) {
    val requestPermissions = intent.getBooleanExtra(EXTRA_REQUEST_CALENDAR_PERMISSION, false)
    var showingErrorMessage by remember {
        mutableStateOf(savedInstanceState == null && intent.hasExtra(EXTRA_ERROR_MESSAGE))
    }
    if (showingErrorMessage) {
        AlertDialog(
            intent.getStringExtra(EXTRA_ERROR_MESSAGE)!!,
            IntentCompat.getSerializableExtra(intent, EXTRA_THROWABLE, Throwable::class.java)
        ) { showingErrorMessage = false }
    }

    val compStartupServices = remember { ServiceLoader.load(ComposableStartupService::class.java) }
    compStartupServices.forEach { service ->
        val show: Boolean by service.shouldShow()
        if (show) service.Content()
    }

    val context = LocalContext.current
    val backStack = rememberNavBackStack(calculateInitialDestination(intent, context))
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    fun goBack(depth: Int = 1) {
        if (backStack.size <= 1) onFinish()
        else repeat(depth) { backStack.removeAt(backStack.lastIndex) }
    }

    WinterEasterEgg()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text("Opcje", modifier = Modifier.padding(16.dp))
                NavigationDrawerItem(
                    label = { Text("Moje kalendarze") },
                    selected = backStack.last() == Destination.SubscriptionList,
                    onClick = {
                        scope.launch { drawerState.close() }
                        backStack.add(Destination.SubscriptionList)
                    },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) }
                )
                NavigationDrawerItem(
                    label = { Text("O programie") },
                    selected = backStack.last() == Destination.Info,
                    onClick = {
                        scope.launch { drawerState.close() }
                        backStack.add(Destination.Info)
                    },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) }
                )
            }
        }
    ) {
        NavDisplay(
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            backStack = backStack,
            onBack = ::goBack,
            entryProvider = entryProvider {
                entry(Destination.Onboarding) {
                    OnboardingScreen(
                        onFinished = {
                            backStack.clear()
                            backStack.add(Destination.Welcome)
                        }
                    )
                }
                entry(Destination.Welcome) {
                    WelcomeScreen(
                        onOpenDrawer = { scope.launch { drawerState.open() } },
                        onScanQrClicked = { backStack.add(Destination.QrScanner) },
                        onManualEntryClicked = { backStack.add(Destination.AddSubscription()) }
                    )
                }
                entry(Destination.QrScanner) {
                    QrScannerScreen(
                        onUrlFound = { url ->
                            goBack()
                            backStack.add(Destination.AddSubscription(url = url))
                        },
                        onBackRequested = ::goBack
                    )
                }
                entry(Destination.SubscriptionList) {
                    SubscriptionsScreen(
                        requestPermissions,
                        onAddRequested = { backStack.add(Destination.Welcome) },
                        onItemSelected = { backStack.add(Destination.EditSubscription(it.id)) },
                        onAboutRequested = { backStack.add(Destination.Info) },
                    )
                }
                entry(Destination.Info) {
                    InfoScreen(
                        compStartupServices,
                        onBackRequested = ::goBack
                    )
                }
                entry<Destination.AddSubscription> { destination ->
                    AddSubscriptionScreen(
                        title = destination.title,
                        color = destination.color,
                        url = destination.url,
                        onBackRequested = { goBack() },
                        onSubscriptionCreated = {
                            backStack.clear()
                            backStack.add(Destination.SubscriptionList)
                        }
                    )
                }
                entry<Destination.EditSubscription> { destination ->
                    val context = LocalContext.current
                    EditSubscriptionScreen(
                        subscriptionId = destination.subscriptionId,
                        onShare = { subscription ->
                            ShareCompat.IntentBuilder(context)
                                .setSubject(subscription.displayName)
                                .setText(subscription.url.toString())
                                .setType("text/plain")
                                .setChooserTitle(R.string.edit_calendar_send_url)
                                .startChooser()
                        },
                        onExit = ::goBack
                    )
                }
            }
        )
    }
}
