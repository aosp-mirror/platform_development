package com.android.sharetest

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.service.chooser.ChooserAction

class CustomActionFactory(private val context: Context) {
    fun getCustomActions(count: Int): Array<ChooserAction> {
        val actions = Array(count) { idx ->
            val customAction = PendingIntent.getBroadcast(
                context,
                idx,
                Intent(BROADCAST_ACTION),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_CANCEL_CURRENT
            )
            ChooserAction.Builder(
                Icon.createWithResource(context, R.drawable.testicon),
                "Action ${idx + 1}",
                customAction
            ).build()
        }

        return actions
    }

    companion object {
        const val BROADCAST_ACTION = "broadcast-action"
    }
}
