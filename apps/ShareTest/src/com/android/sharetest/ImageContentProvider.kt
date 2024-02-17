package com.android.sharetest

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import java.util.Random
import kotlin.math.roundToLong

class ImageContentProvider : ContentProvider() {
    private val random = Random()
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? {
        Thread.sleep(getLatencyMs(getTypeLatency))
        return uri.getQueryParameter(PARAM_TYPE) ?: getTypeFromUri(uri)
    }

    override fun getStreamTypes(uri: Uri, mimeTypeFilter: String): Array<String>? {
        val list = ArrayList<String>(2)
        uri.getQueryParameter(PARAM_TYPE)?.let { list.add(it) }
        getTypeFromUri(uri)?.let { list.add(it) }
        return if (list.isEmpty()) {
            super.getStreamTypes(uri, mimeTypeFilter)
        } else {
            list.toArray(emptyArray())
        }
    }

    private fun getTypeFromUri(uri: Uri): String? {
        if (uri.lastPathSegment?.endsWith("png", true) == true) {
            return "image/png"
        }
        if (uri.lastPathSegment?.endsWith("jpg", true) == true) {
            return "image/jpg"
        }
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int = 0

    override fun openTypedAssetFile(
        uri: Uri,
        mimeTypeFilter: String,
        opts: Bundle?,
    ): AssetFileDescriptor? {
        return openAssetFile(uri, "r")
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor? {
        Thread.sleep(getLatencyMs(openLatency))

        if (shouldFailOpen()) {
            return null
        }

        return uri.lastPathSegment?.let{ context?.assets?.openFd(it) }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? =
        openAssetFile(uri, mode)?.parcelFileDescriptor

    private fun shouldFailOpen() = random.nextFloat() < openFailureRate

    // Provide some gaussian noise around the preferred average latency
    private fun getLatencyMs(avg: Long): Long {
        // Using avg/4 as the standard deviation.
        val noise = avg / 4 * random.nextGaussian()
        return (avg + noise).roundToLong().coerceAtLeast(0)
    }

    companion object {
        fun makeItemUri(idx: Int, mimeType: String): Uri =
            Uri.parse("${URI_PREFIX}img$idx.jpg")
                    .buildUpon()
                    .appendQueryParameter(PARAM_TYPE, mimeType)
                    .build()

        const val IMAGE_COUNT = 8

        const val URI_PREFIX = "content://com.android.sharetest.provider/"
        const val PARAM_TYPE = "type"
        val ICON_URI: Uri = Uri.parse("${URI_PREFIX}letter_a.png")

        var getTypeLatency = 0L
        var openLatency = 0L
        var openFailureRate = 0f
    }
}


