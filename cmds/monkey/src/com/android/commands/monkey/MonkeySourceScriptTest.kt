/*
 * Copyright 2023 The Android Open Source Project
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

package com.android.commands.monkey

import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_INDEX_SHIFT
import android.view.MotionEvent.ACTION_POINTER_UP

import java.io.BufferedWriter
import java.io.FileWriter
import java.io.File
import java.util.Random

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

import org.junit.Test

private fun assertTouchEvent(script: MonkeySourceScript, action: Int) {
    val event = script.getNextEvent()
    assertNotNull(event)
    assertEquals(MonkeyEvent.EVENT_TYPE_TOUCH, event.getEventType())

    val motionEvent = event as MonkeyMotionEvent
    assertEquals(action, motionEvent.getAction())
}

/**
 * Test for class MonkeySourceScript
 */
class MonkeySourceScriptTest {
    companion object {
        const val ACTION_POINTER_2_DOWN = ACTION_POINTER_DOWN.or(1.shl(ACTION_POINTER_INDEX_SHIFT))
    }

    /**
     * Send a PinchZoom command and check the resulting event stream.
     * TODO(b/281806933): fix this incorrect stream (should be POINTER_1_DOWN and add ACTION_UP)
     */
    @Test
    fun pinchZoom() {
        val random = Random()
        val file = File.createTempFile("pinch_zoom", null)
        val fileName = file.getAbsolutePath()
        BufferedWriter(FileWriter(fileName)).use { writer ->
            writer.write("start data >>\n")
            writer.write("PinchZoom(100,100,200,200,50,50,10,10,5)\n")
        }

        val script = MonkeySourceScript(random, fileName, 0, false, 0, 0)

        assertTouchEvent(script, ACTION_DOWN)
        assertTouchEvent(script, ACTION_POINTER_2_DOWN)
        assertTouchEvent(script, ACTION_MOVE)
        assertTouchEvent(script, ACTION_MOVE)
        assertTouchEvent(script, ACTION_MOVE)
        assertTouchEvent(script, ACTION_MOVE)
        assertTouchEvent(script, ACTION_MOVE)
        assertTouchEvent(script, ACTION_POINTER_UP)
        assertNull(script.getNextEvent())

        file.deleteOnExit()
    }
}
