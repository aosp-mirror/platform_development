/*
 * Copyright 2024 The Android Open Source Project
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

package com.android.sharetest

import android.app.AlertDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.Intent.EXTRA_CHOOSER_RESULT_INTENT_SENDER
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.provider.MediaStore
import android.service.chooser.ChooserSession
import android.service.chooser.ChooserSession.ChooserController
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.sharetest.ui.theme.ActivityTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val KEY_SESSION = "chooser-session"
private const val EXTRA_CHOOSER_INTERACTIVE_CALLBACK =
    "com.android.extra.EXTRA_CHOOSER_INTERACTIVE_CALLBACK"

@AndroidEntryPoint(value = FragmentActivity::class)
class InteractiveShareTestActivity : Hilt_InteractiveShareTestActivity() {
    private val TAG = "ShareTest/$hashId"
    private var chooserWindowTopOffset = MutableStateFlow(-1)
    private val isInMultiWindowMode = MutableStateFlow<Boolean>(false)
    private val chooserSession = MutableStateFlow<ChooserSession?>(null)
    private val useRefinementFlow = MutableStateFlow<Boolean>(false)
    private val refinementReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                // Need to show refinement in another activity because this one is beneath the
                // sharesheet.
                val activityIntent =
                    Intent(this@InteractiveShareTestActivity, RefinementActivity::class.java)
                activityIntent.putExtras(intent)
                startActivity(activityIntent)
            }
        }

    private val sessionStateListener =
        object : ChooserSession.ChooserSessionUpdateListener {
            override fun onChooserConnected(
                session: ChooserSession?,
                chooserController: ChooserController?,
            ) {
                Log.d(TAG, "onChooserConnected")
            }

            override fun onChooserDisconnected(session: ChooserSession?) {
                Log.d(TAG, "onChooserDisconnected")
            }

            override fun onSessionClosed(session: ChooserSession?) {
                Log.d(TAG, "onSessionClosed")
                chooserSession.update { oldValue -> if (oldValue === session) null else oldValue }
            }

            override fun onDrawerVerticalOffsetChanged(session: ChooserSession, offset: Int) {
                chooserWindowTopOffset.value = offset
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isInMultiWindowMode.value = isInMultiWindowMode()
        chooserSession.value =
            savedInstanceState?.getParcelable(KEY_SESSION, ChooserSession::class.java)?.apply {
                setChooserStateListener(sessionStateListener)
            }

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chooserSession
                    .scan<ChooserSession?, ChooserSession?>(null) { prevSession, newSession ->
                        prevSession?.setChooserStateListener(null)
                        prevSession?.cancel()
                        newSession?.setChooserStateListener(sessionStateListener)
                        newSession
                    }
                    .collect {}
            }
        }

        val previews = buildList {
            for (i in 0..2) {
                val uri = ImageContentProvider.makeItemUri(i, "image/jpg", true)
                add(Preview(uri, uri, isImage = true))
            }
        }

        setContent {
            var sharedText by remember { mutableStateOf("A text to share") }
            val previewWindowBottom by chooserWindowTopOffset.collectAsStateWithLifecycle(-1)
            val showLaunchInSplitScreen by
                isInMultiWindowMode.map { !it }.collectAsStateWithLifecycle(true)
            val spacing = 5.dp
            val brush = SolidColor(Color.Red)
            // val isChooserRunning by chooserSessionManager.activeSession.map { it != null }
            //     .collectAsStateWithLifecycle(false)
            val isChooserRunning by
                chooserSession.map { it?.isActive == true }.collectAsStateWithLifecycle(false)
            val userRefinement by useRefinementFlow.collectAsStateWithLifecycle(false)
            ActivityTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier.padding(innerPadding),
                        verticalArrangement = Arrangement.spacedBy(spacing),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            Button(onClick = { startCameraApp() }) { Text("Pick Camera App") }
                            Button(onClick = { launchActivity() }) { Text("Launch Activity") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            if (showLaunchInSplitScreen) {
                                Button(onClick = { launchSelfInSplitScreen() }) {
                                    Text("Launch Self in Split-Screen")
                                }
                            }
                            Button(onClick = { launchDialog() }) { Text("Launch Dialog") }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                        ) {
                            TextField(
                                value = sharedText,
                                modifier = Modifier.weight(1f),
                                onValueChange = { sharedText = it },
                            )
                            Button(onClick = { shareText(sharedText) }) { Text("Share Text") }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(spacing)) {
                            if (previews.isNotEmpty()) {
                                Button(onClick = { shareImages(previews, 1) }) {
                                    Text("Share One Image")
                                }
                                if (previews.size > 1) {
                                    Button(onClick = { shareImages(previews, 2) }) {
                                        Text("Share Two Images")
                                    }
                                }
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            modifier = Modifier.clickable { updateRefinement() },
                        ) {
                            Checkbox(
                                checked = userRefinement,
                                onCheckedChange = {},
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                            Text(
                                "Use Refinement",
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                        if (isChooserRunning) {
                            Button(onClick = { closeChooser() }) { Text("Close Chooser") }
                        }
                    }

                    var windowTop by remember { mutableFloatStateOf(0f) }
                    Spacer(
                        modifier =
                            Modifier.fillMaxSize()
                                .onGloballyPositioned { coords ->
                                    windowTop = coords.localToWindow(Offset.Zero).y
                                }
                                .drawBehind {
                                    if (previewWindowBottom >= 0 && isChooserRunning) {
                                        val top = previewWindowBottom.toFloat() - windowTop
                                        drawLine(
                                            brush = brush,
                                            start = Offset(0f, top),
                                            end = Offset(size.width, top),
                                            strokeWidth = 2.dp.toPx(),
                                        )
                                    }
                                }
                    )
                }
            }
        }
    }

    override fun onStart() {
        Log.d(TAG, "onStart")
        super.onStart()
    }

    override fun onResume() {
        Log.d(TAG, "onResume")
        super.onResume()
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        if (useRefinementFlow.value) {
            unregisterReceiver(refinementReceiver)
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState")
        super.onSaveInstanceState(outState)
        chooserSession.value?.let { outState.putParcelable(KEY_SESSION, it) }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG, "onConfigurationChanged")
        super.onConfigurationChanged(newConfig)
    }

    private fun updateRefinement() {
        useRefinementFlow.update {
            if (it) {
                unregisterReceiver(refinementReceiver)
            } else {
                registerReceiver(
                    refinementReceiver,
                    IntentFilter(REFINEMENT_ACTION),
                    RECEIVER_EXPORTED,
                )
            }
            !it
        }
    }

    private fun startCameraApp() {
        val targetIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startOrUpdate(Intent.createChooser(targetIntent, null))
    }

    private fun launchActivity() {
        startActivity(Intent(this, SendTextActivity::class.java))
    }

    private fun launchDialog() {
        val dialog = TestDialog()
        dialog.show(supportFragmentManager, "dialog")
    }

    private fun launchSelfInSplitScreen() {
        startActivity(
            Intent(this, javaClass).apply {
                setFlags(Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun shareText(text: String) {
        val targetIntent =
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, text)
                setType("text/plain")
            }
        val chooserIntent = Intent.createChooser(targetIntent, null)
        startOrUpdate(chooserIntent)
    }

    private fun shareImages(previews: List<Preview>, count: Int) {
        require(count > 0) { "Unexpected count argument value: $count" }
        val targetIntent =
            Intent(if (count == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
                if (count == 1) {
                    putExtra(Intent.EXTRA_STREAM, previews[0].uri)
                } else {
                    putExtra(
                        Intent.EXTRA_STREAM,
                        ArrayList(previews.take(count).map { it.uri }.toList()),
                    )
                }
                clipData =
                    ClipData("image", arrayOf("image/*"), ClipData.Item(previews[0].uri)).apply {
                        previews.take(count).forEachIndexed { idx, item ->
                            if (idx != 0) {
                                addItem(ClipData.Item(item.uri))
                            }
                        }
                    }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setType("image/*")
            }
        val chooserIntent = Intent.createChooser(targetIntent, null)
        startOrUpdate(chooserIntent)
    }

    private fun closeChooser() {
        chooserSession.value?.cancel()
        chooserSession.value = null
        chooserWindowTopOffset.value = -1
    }

    private fun startOrUpdate(chooserIntent: Intent) {
        val chooserController = chooserSession.value?.takeIf { it.isActive }?.chooserController
        if (useRefinementFlow.value) {
            chooserIntent.putExtra(
                Intent.EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER,
                createRefinementIntentSender(this@InteractiveShareTestActivity, true),
            )
        }
        chooserIntent.putExtra(EXTRA_CHOOSER_RESULT_INTENT_SENDER, createResultIntentSender(this))
        if (chooserController == null) {
            val session = ChooserSession()
            chooserSession.value = session
            startActivity(
                Intent(chooserIntent).apply {
                    putExtras(bundleOf(EXTRA_CHOOSER_INTERACTIVE_CALLBACK to session))
                }
            )
        } else {
            chooserController.updateIntent(chooserIntent)
        }
    }
}

class TestDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setMessage("Just a test dialog")
            .setPositiveButton("Close") { _, _ -> dismiss() }
            .create()
    }
}
