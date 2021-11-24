/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.glance.appwidget

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

internal enum class ListAdapterTrampolineType {
    ACTIVITY, BROADCAST, SERVICE, FOREGROUND_SERVICE
}

/**
 * Wraps the "action intent" into an activity trampoline intent, where it will be invoked based on
 * the type, with modifying its content.
 *
 * @see launchTrampolineAction
 */
internal fun Intent.applyTrampolineIntent(
    context: Context,
    viewId: Int,
    type: ListAdapterTrampolineType,
): Intent {
    val target = if (type == ListAdapterTrampolineType.ACTIVITY) {
        ListAdapterTrampolineActivity::class.java
    } else {
        ListAdapterInvisibleTrampolineActivity::class.java
    }
    return Intent(context, target).apply {
        data = Uri.parse(toUri(0))
            .buildUpon()
            .scheme(type.name)
            .path(viewId.toString())
            .build()
        putExtra(ActionTypeKey, type.name)
        putExtra(ActionIntentKey, this@applyTrampolineIntent)
    }
}

/**
 * Unwraps and launches the action intent based on its type.
 *
 * @see applyTrampolineIntent
 */
internal fun Activity.launchTrampolineAction(intent: Intent) {
    val actionIntent = requireNotNull(intent.getParcelableExtra<Intent>(ActionIntentKey)) {
        "List adapter activity trampoline invoked without specifying target intent."
    }
    if (intent.hasExtra(RemoteViews.EXTRA_CHECKED)) {
        actionIntent.putExtra(
            RemoteViews.EXTRA_CHECKED,
            intent.getBooleanExtra(RemoteViews.EXTRA_CHECKED, false)
        )
    }
    val type = requireNotNull(intent.getStringExtra(ActionTypeKey)) {
        "List adapter activity trampoline invoked without trampoline type"
    }
    when (ListAdapterTrampolineType.valueOf(type)) {
        ListAdapterTrampolineType.ACTIVITY -> startActivity(actionIntent)
        ListAdapterTrampolineType.BROADCAST -> sendBroadcast(actionIntent)
        ListAdapterTrampolineType.SERVICE -> startService(actionIntent)
        ListAdapterTrampolineType.FOREGROUND_SERVICE -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ListAdapterTrampolineApi26Impl.startForegroundService(
                    context = this,
                    intent = actionIntent
                )
            } else {
                startService(actionIntent)
            }
        }
    }
    finish()
}

private const val ActionTypeKey = "ACTION_TYPE"
private const val ActionIntentKey = "ACTION_INTENT"

@RequiresApi(Build.VERSION_CODES.O)
private object ListAdapterTrampolineApi26Impl {
    @DoNotInline
    fun startForegroundService(context: Context, intent: Intent) {
        context.startForegroundService(intent)
    }
}
