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
import android.view.MotionEvent.ACTION_UP

import java.io.BufferedWriter
import java.io.FileWriter
import java.io.File
import java.util.Random

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

import org.junit.Test

private fun receiveEvent(script: MonkeySourceScript, type: Int): MonkeyEvent {
    val event = script.getNextEvent()
    assertNotNull("Should receive a valid event", event)
    assertEquals("Wrong event type", type, event.getEventType())
    return event
}

private fun assertTouchEvent(script: MonkeySourceScript, action: Int) {
    val motionEvent = receiveEvent(script, MonkeyEvent.EVENT_TYPE_TOUCH) as MonkeyMotionEvent
    assertEquals(action, motionEvent.action)
}

/**
 * Test for class MonkeySourceScript
 */
class MonkeySourceScriptTest {
    companion object {
        const val ACTION_POINTER_1_DOWN = ACTION_POINTER_DOWN.or(1.shl(ACTION_POINTER_INDEX_SHIFT))
        const val ACTION_POINTER_1_UP = ACTION_POINTER_UP.or(1.shl(ACTION_POINTER_INDEX_SHIFT))
    }

    /**
     * Send a PinchZoom command and check the resulting event stream.
     * Since ACTION_UP is a throttlable event, an event with TYPE_THROTTLE is expected at the end.
     */
    @Test
    fun pinchZoom() {
        val file = File.createTempFile("pinch_zoom", null)
        val fileName = file.absolutePath
        BufferedWriter(FileWriter(fileName)).use { writer ->
            writer.write("start data >>\n")
            writer.write("PinchZoom(100,100,200,200,50,50,10,10,5)\n")
        }

        val script = MonkeySourceScript(Random(), fileName, 0, false, 0, 0)

        assertTouchEvent(script, ACTION_DOWN)
        assertTouchEvent(script, ACTION_POINTER_1_DOWN)
        assertTouchEvent(script, ACTION_MOVE)
        assertTouchEvent(script, ACTION_MOVE)
        assertTouchEvent(script, ACTION_MOVE)
        assertTouchEvent(script, ACTION_MOVE)
        assertTouchEvent(script, ACTION_MOVE)
        assertTouchEvent(script, ACTION_POINTER_1_UP)
        assertTouchEvent(script, ACTION_UP)
        receiveEvent(script, MonkeyEvent.EVENT_TYPE_THROTTLE)
        assertNull(script.getNextEvent())

        file.deleteOnExit()
    }

    /**
     * Send two PressAndHold commands in a row and ensure that the injected stream is consistent.
     */
    @Test
    fun pressAndHoldTwice() {
        val file = File.createTempFile("press_and_hold_twice", null)
        val fileName = file.absolutePath
        BufferedWriter(FileWriter(fileName)).use { writer ->
            writer.write("start data >>\n")
            writer.write("PressAndHold(100,100,10)\n")
            writer.write("PressAndHold(200,200,10)\n")
        }

        val script = MonkeySourceScript(Random(), fileName, 0, false, 0, 0)

        assertTouchEvent(script, ACTION_DOWN)
        receiveEvent(script, MonkeyEvent.EVENT_TYPE_THROTTLE)
        receiveEvent(script, MonkeyEvent.EVENT_TYPE_THROTTLE)
        assertTouchEvent(script, ACTION_UP)
        receiveEvent(script, MonkeyEvent.EVENT_TYPE_THROTTLE)

        assertTouchEvent(script, ACTION_DOWN)
        receiveEvent(script, MonkeyEvent.EVENT_TYPE_THROTTLE)
        receiveEvent(script, MonkeyEvent.EVENT_TYPE_THROTTLE)
        assertTouchEvent(script, ACTION_UP)
        receiveEvent(script, MonkeyEvent.EVENT_TYPE_THROTTLE)
        assertNull(script.getNextEvent())

        file.deleteOnExit()
    }
}
