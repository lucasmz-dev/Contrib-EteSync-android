/*
 * Copyright © Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.etesync.syncadapter

import android.accounts.AccountManager
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import at.bitfire.ical4android.TaskProvider
import at.bitfire.ical4android.TaskProvider.ProviderName
import com.etesync.syncadapter.log.Logger
import com.etesync.syncadapter.resource.LocalTaskList

class PackageChangedReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_PACKAGE_ADDED == intent.action || Intent.ACTION_PACKAGE_FULLY_REMOVED == intent.action) {
            TaskProvider.OPENTASK_PROVIDERS.forEach {
                updateTaskSync(context, it)
            }
        }
    }

    companion object {

        internal fun updateTaskSync(context: Context, provider: ProviderName) {
            val tasksInstalled = LocalTaskList.tasksProviderAvailable(context, provider)
            Logger.log.info("Package (un)installed; ${provider.name} provider now available = $tasksInstalled")

            for (account in AccountManager.get(context).getAccountsByType(App.accountType)) {
                val settings = AccountSettings(context, account)
                val calendarSyncInterval = settings.getSyncInterval(CalendarContract.AUTHORITY)

                if (tasksInstalled) {
                    if (calendarSyncInterval == null) {
                        // do nothing atm
                    } else if (ContentResolver.getIsSyncable(account, provider.authority) <= 0) {
                        ContentResolver.setIsSyncable(account, provider.authority, 1)
                        settings.setSyncInterval(provider.authority, calendarSyncInterval)
                    }
                } else {
                    ContentResolver.setIsSyncable(account, provider.authority, 0)
                }
            }
        }
    }

}
