/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.example.android.vdmdemo.demos

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioRecordingConfiguration
import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Demo activity for testing starting and stopping multiple (NUMBER_OF_RECORDERS) recordings. Does
 * not read nor use the data from the AudioRecorder(s).
 */
class RecorderDemoActivity :
    AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private var audioManager: AudioManager? = null

    private val recorders = MutableList<AudioRecord?>(NUMBER_OF_RECORDERS) { null }

    private val audioRecordingCallback =
        object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
                super.onRecordingConfigChanged(configs)
                Log.d(TAG, "onRecordingConfigChanged with configs: ${configs.map { toLog(it) }}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recorder_demo_activity)

        audioManager = getSystemService(AudioManager::class.java)
        audioManager?.registerAudioRecordingCallback(audioRecordingCallback, null)
    }

    override fun onDestroy() {
        super.onDestroy()

        for (i in 0 until recorders.size) {
            recorders[i]?.run {
                stop()
                release()
            }
            recorders[i] = null
        }
        audioManager?.unregisterAudioRecordingCallback(audioRecordingCallback)
    }

    fun onFirstButtonClick(view: View) {
        onButtonClick(view as Button, 0 /* first recorder index */)
    }

    fun onSecondButtonClick(view: View) {
        onButtonClick(view as Button, 1 /* second recorder index */)
    }

    private fun onButtonClick(button: Button, index: Int) {
        if (index < 0 || index > recorders.size) {
            Log.w(TAG, "Recorder index ($index) out of range (0 - $NUMBER_OF_RECORDERS).")
            return
        }

        if (recorders[index] == null) {
            recorders[index] = createRecorder(index)
        }

        // can still be null if no permissions are granted
        recorders[index]?.let {
            if (toggleRecording(it)) {
                button.setText(R.string.stop_record)
                button.setBackgroundColor(Color.RED)
            } else {
                button.setText(R.string.start_record)
                button.setBackgroundColor(Color.GRAY)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun createRecorder(index: Int): AudioRecord? {
        if (index < 0 || index > RECORDERS_SETTINGS.size) {
            Log.w(
                TAG,
                "Settings for recorder index ($index) out of range (0 - ${RECORDERS_SETTINGS.size}).",
            )
            return null
        }

        if (
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            Log.i("TAG", "Requesting RECORD_AUDIO permission from the user...")
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                PERMISSIONS_REQUEST_CODE,
            )
            return null
        } else {
            with(RECORDERS_SETTINGS[index]) {
                return AudioRecord(
                    source,
                    sampleRate,
                    channels,
                    encoding,
                    AudioRecord.getMinBufferSize(sampleRate, channels, encoding),
                )
            }
        }
    }

    // Start to record if the AudioRecord is stopped
    // or stops the recording if the AudioRecord is recording
    // Returns true if now active and recording, false otherwise
    private fun toggleRecording(recorder: AudioRecord): Boolean {
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            Log.w(TAG, "Recorder $recorder is not initialized!")
            return false
        }

        if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            Log.d(TAG, "Stop recording for recorder: $recorder")
            recorder.stop()
            return false
        } else {
            Log.d(TAG, "Start recording for recorder: $recorder")
            recorder.startRecording()
            return true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("TAG", "RECORD_AUDIO permission granted!")
            } else {
                Log.w("TAG", "RECORD_AUDIO permission NOT granted!")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun toLog(config: AudioRecordingConfiguration) =
        with(config) {
            "AudioRecordingConfiguration: $this has " +
                "audioSource: ${this.audioSource} audioDevice: ${this.audioDevice} " +
                "clientAudioSource: ${this.clientAudioSource} " +
                "clientAudioSessionId: ${this.clientAudioSessionId} " +
                "clientEffects: ${this.clientEffects} effects: ${this.effects} " +
                "isClientSilenced: ${this.isClientSilenced} format: ${this.format}"
        }

    data class RecorderSettings(
        val source: Int,
        val sampleRate: Int,
        val channels: Int,
        val encoding: Int,
    )

    private companion object {
        const val TAG = "RecorderDemoActivity"

        const val PERMISSIONS_REQUEST_CODE = 99
        const val NUMBER_OF_RECORDERS = 2

        // some defaults for easy instantiation of AudioRecorder objects
        const val FIRST_RECORDER_SOURCE = MediaRecorder.AudioSource.MIC
        const val FIRST_RECORDER_SAMPLE_RATE: Int = 8000
        const val FIRST_RECORDER_CHANNELS: Int = AudioFormat.CHANNEL_IN_MONO
        const val FIRST_RECORDER_AUDIO_ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT

        const val SECOND_RECORDER_SOURCE = MediaRecorder.AudioSource.MIC
        const val SECOND_RECORDER_SAMPLE_RATE: Int = 48000
        const val SECOND_RECORDER_CHANNELS: Int = AudioFormat.CHANNEL_IN_STEREO
        const val SECOND_RECORDER_AUDIO_ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT

        private val RECORDERS_SETTINGS =
            listOf(
                RecorderSettings(
                    FIRST_RECORDER_SOURCE,
                    FIRST_RECORDER_SAMPLE_RATE,
                    FIRST_RECORDER_CHANNELS,
                    FIRST_RECORDER_AUDIO_ENCODING,
                ),
                RecorderSettings(
                    SECOND_RECORDER_SOURCE,
                    SECOND_RECORDER_SAMPLE_RATE,
                    SECOND_RECORDER_CHANNELS,
                    SECOND_RECORDER_AUDIO_ENCODING,
                ),
            )
    }
}
