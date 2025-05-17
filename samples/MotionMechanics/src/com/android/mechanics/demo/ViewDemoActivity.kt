/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.mechanics.demo

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.android.mechanics.spec.Mapping
import com.android.mechanics.spec.MotionSpec
import com.android.mechanics.spec.builder.directionalMotionSpec
import com.android.mechanics.spring.SpringParameters
import com.android.mechanics.view.DistanceGestureContext
import com.android.mechanics.view.ViewMotionValue

class ViewDemoActivity : AppCompatActivity() {

    private lateinit var slider: SeekBar
    private lateinit var box: View
    private lateinit var rootLayout: ConstraintLayout
    private lateinit var dropdown: Spinner
    private var specs = createMotionSpecs(100f)

    private val gestureContext by lazy { DistanceGestureContext.create(this) }
    private val motionValue by lazy { ViewMotionValue(0f, gestureContext, MotionSpec.Empty) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_demo)

        slider = findViewById(R.id.slider)
        box = findViewById(R.id.box)
        rootLayout = findViewById(R.id.rootLayout)
        dropdown = findViewById(R.id.dropdown)
        dropdown.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    val key = checkNotNull(parent?.adapter?.getItem(position) as String)

                    motionValue.spec = checkNotNull(specs[key])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        var maxRange = -1f

        slider.viewTreeObserver.addOnGlobalLayoutListener {
            val newWidth = slider.width.toFloat() - slider.thumb.bounds.width()
            if (maxRange != newWidth) {
                val percentage = slider.progress.toFloat() / slider.max.toFloat()
                maxRange = newWidth

                slider.max = maxRange.toInt()
                slider.progress = (maxRange * percentage).toInt()

                specs = createMotionSpecs(maxRange)

                dropdown.adapter =
                    ArrayAdapter(this, android.R.layout.simple_spinner_item, specs.keys.toList())
                        .apply {
                            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        }
            }
        }

        slider.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) {
                        gestureContext.dragOffset = progress.toFloat()
                        motionValue.input = progress.toFloat()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )

        motionValue.addUpdateCallback {
            val layoutParams = box.layoutParams as ConstraintLayout.LayoutParams
            layoutParams.leftMargin = motionValue.output.toInt()
            box.layoutParams = layoutParams
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        motionValue.dispose()
    }

    fun createMotionSpecs(maxRange: Float): Map<String, MotionSpec> {
        return mapOf(
            "empty" to MotionSpec.Empty,
            "toggle" to
                MotionSpec(
                    directionalMotionSpec(DefaultSpring, Mapping.Zero) {
                        fixedValue(breakpoint = maxRange / 2f, value = maxRange)
                    }
                ),
        )
    }

    companion object {
        val DefaultSpring = SpringParameters(700f, 0.8f)
    }
}
