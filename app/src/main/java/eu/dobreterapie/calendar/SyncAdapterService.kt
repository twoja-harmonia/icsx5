/*
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 */

package eu.dobreterapie.calendar

import android.app.Service
import android.content.Intent

class SyncAdapterService: Service() {

    private val syncAdapter: SyncAdapter by lazy { SyncAdapter(this) }

    override fun onBind(intent: Intent?) = syncAdapter.syncAdapterBinder!!

}
