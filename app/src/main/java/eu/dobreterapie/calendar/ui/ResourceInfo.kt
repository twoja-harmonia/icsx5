/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package eu.dobreterapie.calendar.ui

import android.net.Uri

data class ResourceInfo(

    var uri: Uri,

    var exception: Exception? = null,

    var calendarName: String? = null,
    var calendarColor: Int? = null,
    var eventsFound: Int = 0

)