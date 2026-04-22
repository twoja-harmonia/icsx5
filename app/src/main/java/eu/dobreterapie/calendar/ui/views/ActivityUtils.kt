/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package eu.dobreterapie.calendar.ui.views

import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat

fun ComponentActivity.configureEdgeToEdge() {
    enableEdgeToEdge()
    WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
}
