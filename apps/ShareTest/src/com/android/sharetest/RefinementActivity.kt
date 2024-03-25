package com.android.sharetest

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.ResultReceiver

class RefinementActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val resultReceiver =
            intent.getParcelableExtra(Intent.EXTRA_RESULT_RECEIVER, ResultReceiver::class.java)
        val sharedIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder
                .setMessage(
                    """
                    |Is modified by payload selection: ${!intent.isInitial}
                    |
                    |Complete the share?
                    """.trimMargin()
                )
                .setTitle("Refinement invoked!")
                .setPositiveButton("Yes") { _, _ ->
                    val bundle = Bundle().apply {
                        putParcelable(Intent.EXTRA_INTENT, sharedIntent)
                    }
                    resultReceiver?.send(RESULT_OK, bundle)
                    finish()
                }
                .setNegativeButton("No") { _, _ ->
                    resultReceiver?.send(RESULT_CANCELED, null)
                    finish()
                }
                .setOnCancelListener {
                    resultReceiver?.send(RESULT_CANCELED, null)
                    finish()
                }

        builder.create().show()
    }
}
