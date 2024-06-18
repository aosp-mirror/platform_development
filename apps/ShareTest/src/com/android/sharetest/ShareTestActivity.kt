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

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.android.sharetest.ImageContentProvider.Companion.IMAGE_COUNT
import com.android.sharetest.ImageContentProvider.Companion.makeItemUri
import kotlin.random.Random

private const val TYPE_IMAGE = "Image"
private const val TYPE_VIDEO = "Video"
private const val TYPE_PDF = "PDF Doc"
private const val TYPE_IMG_VIDEO = "Image / Video Mix"
private const val TYPE_IMG_PDF = "Image / PDF Mix"
private const val TYPE_VIDEO_PDF = "Video / PDF Mix"
private const val TYPE_ALL = "All Type Mix"
private const val ADDITIONAL_ITEM_COUNT = 1_000

@RequiresApi(34)
class ShareTestActivity : Activity() {
    private lateinit var customActionReceiver: BroadcastReceiver
    private lateinit var refinementReceiver: BroadcastReceiver
    private lateinit var mediaSelection: RadioGroup
    private lateinit var textSelection: RadioGroup
    private lateinit var mediaTypeSelection: Spinner
    private lateinit var mediaTypeHeader: View
    private lateinit var richText: CheckBox
    private lateinit var albumCheck: CheckBox
    private lateinit var metadata: EditText
    private lateinit var shareouselCheck: CheckBox
    private lateinit var altIntentCheck: CheckBox
    private lateinit var callerTargetCheck: CheckBox
    private lateinit var selectionLatencyGroup: RadioGroup
    private lateinit var imageSizeMetadataCheck: CheckBox
    private val customActionFactory = CustomActionFactory(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val container = requireViewById<View>(R.id.container)
        ViewCompat.setOnApplyWindowInsetsListener(container) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<MarginLayoutParams> {
                leftMargin = insets.left
                topMargin = insets.top
                rightMargin = insets.right
                bottomMargin = insets.bottom
            }

            WindowInsetsCompat.CONSUMED
        }

        customActionReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                Toast.makeText(
                    this@ShareTestActivity,
                    "Custom action invoked, isModified: ${!intent.isInitial}",
                    Toast.LENGTH_LONG
                )
                    .show()
            }
        }

        refinementReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                // Need to show refinement in another activity because this one is beneath the
                // sharesheet.
                val activityIntent = Intent(this@ShareTestActivity, RefinementActivity::class.java)
                activityIntent.putExtras(intent)
                startActivity(activityIntent)
            }
        }

        registerReceiver(
            customActionReceiver,
            IntentFilter(CustomActionFactory.BROADCAST_ACTION),
            Context.RECEIVER_EXPORTED
        )

        registerReceiver(
            refinementReceiver,
            IntentFilter(REFINEMENT_ACTION),
            Context.RECEIVER_EXPORTED
        )

        richText = requireViewById(R.id.use_rich_text)
        albumCheck = requireViewById(R.id.album_text)
        shareouselCheck = requireViewById(R.id.shareousel)
        altIntentCheck = requireViewById(R.id.alt_intent)
        callerTargetCheck = requireViewById(R.id.caller_direct_target)
        mediaTypeSelection = requireViewById(R.id.media_type_selection)
        mediaTypeHeader = requireViewById(R.id.media_type_header)
        selectionLatencyGroup = requireViewById(R.id.selection_latency)
        imageSizeMetadataCheck = requireViewById(R.id.image_size_metadata)
        mediaSelection = requireViewById<RadioGroup>(R.id.media_selection).apply {
            setOnCheckedChangeListener { _, id -> updateMediaTypesList(id) }
            check(R.id.no_media)
        }
        metadata = requireViewById(R.id.metadata)

        textSelection = requireViewById<RadioGroup>(R.id.text_selection).apply {
            check(R.id.short_text)
        }
        requireViewById<RadioGroup>(R.id.action_selection).check(R.id.no_actions)

        requireViewById<Button>(R.id.share).setOnClickListener(this::share)

        requireViewById<RadioButton>(R.id.no_media).setOnClickListener {
            if (textSelection.checkedRadioButtonId == R.id.no_text) {
                textSelection.check(R.id.short_text)
            }
        }

        requireViewById<RadioGroup>(R.id.image_latency).setOnCheckedChangeListener { _, checkedId ->
            ImageContentProvider.openLatency = when (checkedId) {
                R.id.image_latency_50 -> 50
                R.id.image_latency_200 -> 200
                R.id.image_latency_800 -> 800
                else -> 0
            }
        }
        requireViewById<RadioGroup>(R.id.image_latency).check(R.id.image_latency_none)

        requireViewById<RadioGroup>(R.id.image_get_type_latency).setOnCheckedChangeListener {
                _,
                checkedId,
            ->
            ImageContentProvider.getTypeLatency = when (checkedId) {
                R.id.image_get_type_latency_50 -> 50
                R.id.image_get_type_latency_200 -> 200
                R.id.image_get_type_latency_800 -> 800
                else -> 0
            }
        }
        requireViewById<RadioGroup>(R.id.image_get_type_latency).check(
            R.id.image_get_type_latency_none
        )

        requireViewById<RadioGroup>(R.id.image_query_latency).let { radioGroup ->
            radioGroup.setOnCheckedChangeListener {
                    _,
                    checkedId,
                ->
                ImageContentProvider.queryLatency = when (checkedId) {
                    R.id.image_query_latency_50 -> 50
                    R.id.image_query_latency_200 -> 200
                    R.id.image_query_latency_800 -> 800
                    else -> 0
                }
            }
            radioGroup.check(R.id.image_query_latency_none)
        }

        requireViewById<RadioGroup>(R.id.image_load_failure_rate).setOnCheckedChangeListener {
                _,
                checkedId,
            ->
            ImageContentProvider.openFailureRate = when (checkedId) {
                R.id.image_load_failure_rate_50 -> .5f
                R.id.image_load_failure_rate_100 -> 1f
                else -> 0f
            }
        }
        requireViewById<RadioGroup>(R.id.image_load_failure_rate).check(
            R.id.image_load_failure_rate_none
        )
    }

    private fun updateMediaTypesList(id: Int) {
        when (id) {
            R.id.no_media -> removeMediaTypeOptions()
            R.id.one_image -> setSingleMediaTypeOptions()
            R.id.many_images -> setAllMediaTypeOptions()
        }
    }

    private fun removeMediaTypeOptions() {
        mediaTypeSelection.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_item, emptyArray<String>()
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        setMediaTypeVisibility(false)
    }

    private fun setSingleMediaTypeOptions() {
        mediaTypeSelection.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            arrayOf(TYPE_IMAGE, TYPE_VIDEO, TYPE_PDF)
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        setMediaTypeVisibility(true)
    }

    private fun setAllMediaTypeOptions() {
        mediaTypeSelection.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            arrayOf(
                TYPE_IMAGE,
                TYPE_VIDEO,
                TYPE_PDF,
                TYPE_IMG_VIDEO,
                TYPE_IMG_PDF,
                TYPE_VIDEO_PDF,
                TYPE_ALL
            )
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        setMediaTypeVisibility(true)
    }

    private fun setMediaTypeVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.GONE
        mediaTypeHeader.visibility = visibility
        mediaTypeSelection.visibility = visibility
        shareouselCheck.visibility = visibility
        altIntentCheck.visibility = visibility
    }

    private fun share(view: View) {
        val share = Intent(Intent.ACTION_SEND)
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val mimeTypes = getSelectedContentTypes()

        val imageIndex = Random.nextInt(ADDITIONAL_ITEM_COUNT)

        when (mediaSelection.checkedRadioButtonId) {
            R.id.one_image -> share.apply {
                val sharedUri = makeItemUri(
                    imageIndex,
                    mimeTypes[imageIndex % mimeTypes.size],
                    imageSizeMetadataCheck.isChecked
                )
                putExtra(Intent.EXTRA_STREAM, sharedUri)
                clipData = ClipData("", arrayOf("image/jpg"), ClipData.Item(sharedUri))
                type = if (mimeTypes.size == 1) mimeTypes[0] else "*/*"
            }

            R.id.many_images -> share.apply {
                val imageUris = ArrayList(
                    (0 until IMAGE_COUNT).map { idx ->
                        makeItemUri(
                            idx,
                            mimeTypes[idx % mimeTypes.size],
                            imageSizeMetadataCheck.isChecked
                        )
                    })
                action = Intent.ACTION_SEND_MULTIPLE
                clipData = ClipData("", arrayOf("image/jpg"), ClipData.Item(imageUris[0])).apply {
                    for (i in 1 until IMAGE_COUNT) {
                        addItem(ClipData.Item(imageUris[i]))
                    }
                }
                type = if (mimeTypes.size == 1) mimeTypes[0] else "*/*"
                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    imageUris
                )
            }
        }

        val url = "https://developer.android.com/training/sharing/send#adding-rich-content-previews"

        when (textSelection.checkedRadioButtonId) {
            R.id.short_text -> share.setText(createShortText())
            R.id.long_text -> share.setText(createLongText())
            R.id.url_text -> share.setText(url)
        }

        if (requireViewById<CheckBox>(R.id.include_title).isChecked) {
            share.putExtra(Intent.EXTRA_TITLE, createTextTitle())
        }

        if (requireViewById<CheckBox>(R.id.include_icon).isChecked) {
            share.clipData = ClipData(
                "", arrayOf("image/png"), ClipData.Item(ImageContentProvider.ICON_URI)
            )
            share.data = ImageContentProvider.ICON_URI
        }

        val chosenComponentPendingIntent = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, ChosenComponentBroadcastReceiver::class.java),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val chooserIntent =
            Intent.createChooser(share, null, chosenComponentPendingIntent.intentSender)

        val sendingImage = mediaSelection.checkedRadioButtonId.let {
            it == R.id.one_image || it == R.id.many_images
        }
        if (sendingImage && altIntentCheck.isChecked) {
            chooserIntent.putExtra(
                Intent.EXTRA_ALTERNATE_INTENTS,
                arrayOf(createAlternateIntent(share))
            )
        }
        if (callerTargetCheck.isChecked) {
            chooserIntent.putExtra(
                Intent.EXTRA_CHOOSER_TARGETS,
                arrayOf(createCallerTarget(this, "Initial Direct Target"))
            )
        }

        if (albumCheck.isChecked) {
            chooserIntent.putExtra(
                Intent.EXTRA_CHOOSER_CONTENT_TYPE_HINT,
                Intent.CHOOSER_CONTENT_TYPE_ALBUM
            )
        }

        if (requireViewById<CheckBox>(R.id.include_modify_share).isChecked) {
            chooserIntent.setModifyShareAction(this)
        }

        if (requireViewById<CheckBox>(R.id.use_refinement).isChecked) {
            chooserIntent.putExtra(
                Intent.EXTRA_CHOOSER_REFINEMENT_INTENT_SENDER,
                createRefinementIntentSender(this, true)
            )
        }

        when (requireViewById<RadioGroup>(R.id.action_selection).checkedRadioButtonId) {
            R.id.one_action -> chooserIntent.putExtra(
                Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, customActionFactory.getCustomActions(1)
            )

            R.id.five_actions -> chooserIntent.putExtra(
                Intent.EXTRA_CHOOSER_CUSTOM_ACTIONS, customActionFactory.getCustomActions(5)
            )
        }

        if (metadata.text.isNotEmpty()) {
            chooserIntent.putExtra(Intent.EXTRA_METADATA_TEXT, metadata.text)
        }
        if (shareouselCheck.isChecked) {
            val additionalContentUri =
                AdditionalContentProvider.ADDITIONAL_CONTENT_URI.buildUpon()
                        .appendQueryParameter(
                            AdditionalContentProvider.PARAM_COUNT,
                            ADDITIONAL_ITEM_COUNT.toString(),
                        )
                        .appendQueryParameter(
                            AdditionalContentProvider.PARAM_SIZE_META,
                            imageSizeMetadataCheck.isChecked.toString(),
                        )
                        .also { builder ->
                            mimeTypes.forEach {
                                builder.appendQueryParameter(
                                    AdditionalContentProvider.PARAM_MIME_TYPE, it)
                            }
                        }
                        .build()
            chooserIntent.putExtra(
                Intent.EXTRA_CHOOSER_ADDITIONAL_CONTENT_URI,
                additionalContentUri,
            )
            chooserIntent.putExtra(Intent.EXTRA_CHOOSER_FOCUSED_ITEM_POSITION, 0)
            chooserIntent.clipData?.addItem(ClipData.Item(additionalContentUri))
            if (mediaSelection.checkedRadioButtonId == R.id.one_image) {
                chooserIntent.putExtra(
                    AdditionalContentProvider.CURSOR_START_POSITION,
                    imageIndex,
                )
            }
            val latency = when (selectionLatencyGroup.checkedRadioButtonId) {
                R.id.selection_latency_50 -> 50
                R.id.selection_latency_200 -> 200
                R.id.selection_latency_800 -> 800
                else -> 0
            }
            if (latency > 0) {
                chooserIntent.putExtra(AdditionalContentProvider.EXTRA_SELECTION_LATENCY, latency)
            }
        }

        startActivity(chooserIntent)
    }

    private fun getSelectedContentTypes(): Array<String> =
        mediaTypeSelection.selectedItem?.let { types ->
            when (types) {
                TYPE_VIDEO -> arrayOf("video/mp4")
                TYPE_PDF -> arrayOf("application/pdf")
                TYPE_IMG_VIDEO -> arrayOf("image/jpeg", "video/mp4")
                TYPE_IMG_PDF -> arrayOf("image/jpeg", "application/pdf")
                TYPE_VIDEO_PDF -> arrayOf("video/mp4", "application/pdf")
                TYPE_ALL -> arrayOf("image/jpeg", "video/mp4", "application/pdf")
                else -> null
            }
        } ?: arrayOf("image/jpeg")

    private fun createShortText(): CharSequence =
        SpannableStringBuilder()
            .append("This", StyleSpan(Typeface.BOLD), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            .append(" is ", StyleSpan(Typeface.ITALIC), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            .append("a bit of ")
            .append("text", BackgroundColorSpan(Color.YELLOW), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            .append(" to ")
            .append("share", ForegroundColorSpan(Color.GREEN), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            .append(".")
            .let {
                if (richText.isChecked) it else it.toString()
            }

    private fun createLongText(): CharSequence =
        SpannableStringBuilder("Here is a lot more text to share:")
            .apply {
                val colors =
                    arrayOf(
                        Color.RED,
                        Color.GREEN,
                        Color.BLUE,
                        Color.CYAN,
                        Color.MAGENTA,
                        Color.YELLOW,
                        Color.BLACK,
                        Color.DKGRAY,
                        Color.GRAY,
                    )
                for (color in colors) {
                    append("\n")
                    append(
                        createShortText(), BulletSpan(40, color, 20),
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE
                    )
                }
            }
            .let {
                if (richText.isChecked) it else it.toString()
            }

    private fun createTextTitle(): CharSequence =
        SpannableStringBuilder()
            .append("Here's", UnderlineSpan(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            .append(" the ", StyleSpan(Typeface.ITALIC), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            .append("Title", ForegroundColorSpan(Color.RED), Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
            .append("!")
            .let {
                if (richText.isChecked) it else it.toString()
            }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(customActionReceiver)
        unregisterReceiver(refinementReceiver)
    }
}


