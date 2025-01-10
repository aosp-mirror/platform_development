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
import android.media.MediaRecorder.AudioSource
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.concurrent.thread
import kotlinx.atomicfu.AtomicBoolean
import kotlinx.atomicfu.atomic

/**
 * Demo activity for testing starting and stopping multiple (NUMBER_OF_RECORDERS) recordings. Does
 * not use the data from the AudioRecorder(s). The reading from the AudioRecord(s) is necessary to
 * activate the dynamic policies.
 */
class RecorderDemoActivity :
    AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private var audioManager: AudioManager? = null

    private val recorders = List(NUMBER_OF_RECORDERS) { AudioRecorder(it) }

    private lateinit var buttons: List<Button>
    private lateinit var recorderStatusTextViews: List<TextView>

    private val audioRecordingCallback =
        object : AudioManager.AudioRecordingCallback() {
            override fun onRecordingConfigChanged(configs: List<AudioRecordingConfiguration>) {
                super.onRecordingConfigChanged(configs)
                Log.d(TAG, "onRecordingConfigChanged with configs: ${configs.map { toLog(it) }}")

                // recording configuration changed, update UI state for all recorders
                runOnUiThread { updateAllRecordersUi() }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.recorder_demo_activity)

        buttons =
            listOf(
                requireViewById(R.id.first_recorder_button),
                requireViewById(R.id.second_recorder_button),
            )

        recorderStatusTextViews =
            listOf(
                requireViewById(R.id.first_recorder_status),
                requireViewById(R.id.second_recorder_status),
            )

        audioManager = getSystemService(AudioManager::class.java)
        audioManager?.registerAudioRecordingCallback(audioRecordingCallback, null)
    }

    override fun onDestroy() {
        super.onDestroy()

        recorders.forEach { it.stopRecording() }

        audioManager?.unregisterAudioRecordingCallback(audioRecordingCallback)
    }

    fun onFirstButtonClick(view: View) {
        onButtonClick(0 /* first recorder index */)
    }

    fun onSecondButtonClick(view: View) {
        onButtonClick(1 /* second recorder index */)
    }

    private fun onButtonClick(index: Int) {
        if (index < 0 || index > recorders.size) {
            Log.w(TAG, "Recorder index ($index) out of range (0 - $NUMBER_OF_RECORDERS).")
            return
        }

        recorders[index].toggleRecording()
        // just toggle the recording, the UI will be later updated by the recording config callback
    }

    private fun updateAllRecordersUi() {
        for (i in 0 until NUMBER_OF_RECORDERS) {
            updateRecorderUi(i)
        }
    }

    private fun updateRecorderUi(index: Int) {
        recorders[index].run {
            val isRecording = isRecording()
            val buttonText =
                if (isRecording) {
                    R.string.stop_record
                } else {
                    R.string.start_record
                }
            val backgroundColor =
                if (isRecording) {
                    Color.RED
                } else {
                    Color.GRAY
                }

            buttons[index].setText(buttonText)
            buttons[index].setBackgroundColor(backgroundColor)
            recorderStatusTextViews[index].text = getRecorderStatus()
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
            "AudioRecordingConfiguration: $this has audioSource: ${this.audioSource} audioDevice: ${this.audioDevice} clientAudioSource: ${this.clientAudioSource} clientAudioSessionId: ${this.clientAudioSessionId} clientEffects: ${this.clientEffects} effects: ${this.effects} isClientSilenced: ${this.isClientSilenced} format: ${this.format}"
        }

    /**
     * Utility class managing creation and reading from an AudioRecord. Doesn't do anything with the
     * recorded audio data.
     */
    private inner class AudioRecorder(private val index: Int) {
        private var isRunning: AtomicBoolean = atomic(false)
        private var audioRecord: AudioRecord? = null

        private fun createAndReadAudioRecord() {
            createRecorder(index)?.let { record ->
                audioRecord = record

                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "Can NOT start recording for UNINITIALIZED AudioRecord $index.")
                    return
                }

                val bufferSize =
                    (AUDIO_RECORDER_BUFFER_SIZE_MS * record.sampleRate * record.channelCount / 1000)
                val buffer = ByteArray(bufferSize)

                Log.d(TAG, "AudioRecord $index start recording...")
                try {
                    record.startRecording()
                } catch (e: Exception) {
                    Log.e(TAG, "Exception starting AudioRecord $index.", e)
                    return
                }
                Log.d(TAG, "AudioRecord $index recording started.")

                isRunning.value = true

                while (isRunning.value) {
                    val ret = record.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                    if (ret < 0) {
                        Log.e(
                            TAG,
                            "Error calling read on AudioRecord $index, read call returned $ret",
                        )
                        break
                    }
                    // no use for the audio data
                }

                // No longer running, recording should stop and be released,
                // it will be recreated when needed
                record.stop()
                record.release()
                audioRecord = null

                Log.d(TAG, "AudioRecord $index stopped recording.")
            } ?: Log.e(TAG, "Can NOT start recording for NULL AudioRecord $index.")
        }

        fun isRecording() = isRunning.value

        fun startRecording() {
            Log.d(TAG, "startRecording() for AudioRecord $index.")

            thread { createAndReadAudioRecord() }
        }

        fun stopRecording() {
            Log.d(TAG, "stopRecording() for AudioRecord $index.")
            isRunning.value = false
        }

        fun toggleRecording() {
            if (isRecording()) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        fun getRecorderStatus() =
            audioRecord?.let {
                val state =
                    if (it.state == AudioRecord.STATE_INITIALIZED) {
                        if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            "Recording"
                        } else {
                            "Stopped"
                        }
                    } else {
                        "Uninitialized"
                    }

                "$state source ${friendlyAudioSource(it.audioSource)} device address ${it.routedDevice?.address}"
            } ?: "Recorder not created!"

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
                ContextCompat.checkSelfPermission(
                    this@RecorderDemoActivity,
                    Manifest.permission.RECORD_AUDIO,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.i("TAG", "Requesting RECORD_AUDIO permission from the user...")
                ActivityCompat.requestPermissions(
                    this@RecorderDemoActivity,
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
    }

    private data class RecorderSettings(
        val source: Int,
        val sampleRate: Int,
        val channels: Int,
        val encoding: Int,
    )

    private companion object {
        const val TAG = "RecorderDemoActivity"

        const val PERMISSIONS_REQUEST_CODE = 99
        const val NUMBER_OF_RECORDERS = 2

        const val AUDIO_RECORDER_BUFFER_SIZE_MS = 50

        // some defaults for easy instantiation of AudioRecorder objects
        const val FIRST_RECORDER_SOURCE = AudioSource.DEFAULT
        const val FIRST_RECORDER_SAMPLE_RATE: Int = 8000
        const val FIRST_RECORDER_CHANNELS: Int = AudioFormat.CHANNEL_IN_MONO
        const val FIRST_RECORDER_AUDIO_ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT

        const val SECOND_RECORDER_SOURCE = AudioSource.MIC
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

        private fun friendlyAudioSource(source: Int) =
            when (source) {
                AudioSource.DEFAULT -> "DEFAULT"
                AudioSource.MIC -> "MIC"
                AudioSource.VOICE_UPLINK -> "VOICE_UPLINK"
                AudioSource.VOICE_DOWNLINK -> "VOICE_DOWNLINK"
                AudioSource.VOICE_CALL -> "VOICE_CALL"
                AudioSource.CAMCORDER -> "CAMCORDER"
                AudioSource.VOICE_RECOGNITION -> "VOICE_RECOGNITION"
                AudioSource.VOICE_COMMUNICATION -> "VOICE_COMMUNICATION"
                AudioSource.REMOTE_SUBMIX -> "REMOTE_SUBMIX"
                AudioSource.UNPROCESSED -> "UNPROCESSED"
                1997 -> "ECHO_REFERENCE" /* AudioSource.ECHO_REFERENCE */
                AudioSource.VOICE_PERFORMANCE -> "VOICE_PERFORMANCE"
                1998 -> "RADIO_TUNER" /* AudioSource.RADIO_TUNER */
                1999 -> "HOTWORD" /* AudioSource.HOTWORD */
                2000 -> "ULTRASOUND" /* AudioSource.ULTRASOUND */
                -1 -> "AUDIO_SOURCE_INVALID" /* AudioSource.AUDIO_SOURCE_INVALID */
                else -> "unknown source $source"
            }
    }
}
