package eu.dobreterapie.calendar.ui.screen

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.dobreterapie.calendar.R

private const val ONBOARDING_PREFS = "onboarding_prefs"
private const val KEY_ONBOARDING_DONE = "onboarding_done"

fun isOnboardingCompleted(context: Context): Boolean {
    val prefs = context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE)
    return prefs.getBoolean(KEY_ONBOARDING_DONE, false)
}

private fun setOnboardingCompleted(context: Context) {
    val prefs = context.getSharedPreferences(ONBOARDING_PREFS, Context.MODE_PRIVATE)
    prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
}

private const val PAGE_COUNT = 4

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    var currentPage by remember { mutableIntStateOf(0) }
    var calendarGranted by remember { mutableStateOf(false) }
    var batteryOptimizationDisabled by remember { mutableStateOf(false) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        calendarGranted = permissions.values.all { it }
        if (calendarGranted) currentPage = 2
    }

    // Check battery optimization status on resume
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        batteryOptimizationDisabled = pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val step = 40.dp.toPx()
                    val strokeWidth = 1.dp.toPx()
                    val lineColor = Color.White.copy(alpha = 0.35f)
                    var x = 0f
                    while (x < size.width) {
                        drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), strokeWidth)
                        x += step
                    }
                    var y = 0f
                    while (y < size.height) {
                        drawLine(lineColor, Offset(0f, y), Offset(size.width, y), strokeWidth)
                        y += step
                    }
                }
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (currentPage < PAGE_COUNT - 1) {
                    TextButton(
                        onClick = {
                            setOnboardingCompleted(context)
                            onFinished()
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                        )
                    ) {
                        Text("Pomiń")
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Animated page content
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                label = "onboarding_page"
            ) { page ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (page) {
                        0 -> {
                            Image(
                                painter = painterResource(id = R.mipmap.ic_launcher_foreground),
                                contentDescription = "Logo",
                                modifier = Modifier.size(160.dp)
                            )
                        }
                        1 -> {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calendar_today),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        2 -> {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_battery_alert),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        3 -> {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_check_circle),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = when (page) {
                            0 -> "DobreTerapie Sync"
                            1 -> "Dostęp do kalendarza"
                            2 -> "Optymalizacja baterii"
                            3 -> "Gotowe!"
                            else -> ""
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = when (page) {
                            0 -> "Synchronizuj wizyty z dobreterapie.eu automatycznie na swoim telefonie."
                            1 -> "Potrzebujemy dostępu do kalendarza, aby zapisywać Twoje wizyty."
                            2 -> "Wyłącz optymalizację baterii, aby wizyty synchronizowały się na czas.\n\nBez tego terminy mogą się nie aktualizować."
                            3 -> "Zeskanuj kod QR od terapeuty lub wpisz link ręcznie, aby dodać swój pierwszy kalendarz."
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Dots indicator
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(PAGE_COUNT) { index ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (index == currentPage) 12.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == currentPage)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main button
            Button(
                onClick = {
                    when (currentPage) {
                        0 -> currentPage = 1
                        1 -> {
                            if (calendarGranted) {
                                currentPage = 2
                            } else {
                                calendarPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_CALENDAR,
                                        Manifest.permission.WRITE_CALENDAR
                                    )
                                )
                            }
                        }
                        2 -> {
                            if (batteryOptimizationDisabled) {
                                currentPage = 3
                            } else {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                                // Will update batteryOptimizationDisabled on recompose
                                currentPage = 3
                            }
                        }
                        3 -> {
                            setOnboardingCompleted(context)
                            onFinished()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = when (currentPage) {
                        0 -> "Zaczynamy"
                        1 -> if (calendarGranted) "Dalej" else "Zezwól na dostęp"
                        2 -> if (batteryOptimizationDisabled) "Dalej" else "Wyłącz optymalizację"
                        3 -> "Dodaj kalendarz"
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Secondary skip button for optional permission pages only (battery)
            if (currentPage == 2) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { currentPage = 3 },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                    )
                ) {
                    Text("Pomiń (niezalecane)")
                }
            }

            // Info text for required permission
            if (currentPage == 1 && !calendarGranted) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "* Dostęp do kalendarza jest wymagany do działania aplikacji",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
