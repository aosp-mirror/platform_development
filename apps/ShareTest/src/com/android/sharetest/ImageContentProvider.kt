package com.android.sharetest

import android.content.ContentProvider
import android.content.ContentValues
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Size

class ImageContentProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? {
        SystemClock.sleep(getLatencyMs(queryLatency))
        val includeSize = runCatching {
            uri.getQueryParameter(PARAM_SIZE_META).toBoolean()
        }.getOrDefault(true)
        if (!includeSize) return null
        val size = getImageSize(uri) ?: return null
        return MatrixCursor(
            arrayOf(
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT
            )
        ).apply {
            addRow(arrayOf(size.width, size.height))
        }
    }

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
        selectionArgs: Array<String>?,
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

        return uri.lastPathSegment?.let { context?.assets?.openFd(it) }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? =
        openAssetFile(uri, mode)?.parcelFileDescriptor

    private fun shouldFailOpen() = getRandomFailure(openFailureRate)

    companion object {
        private val sizeMap = mapOf(
            "img1.jpg" to Size(1944, 2592),
            "img2.jpg" to Size(2368, 3200),
            "img3.jpg" to Size(3200, 2368),
            "img4.jpg" to Size(4032, 3024),
            "img5.jpg" to Size(2448, 3264),
            "img6.jpg" to Size(4032, 3024),
            "img7.jpg" to Size(3024, 4032),
            "img8.jpg" to Size(1600, 1200),
        )

        fun makeItemUri(idx: Int, mimeType: String, includeSize: Boolean): Uri =
            Uri.parse("${URI_PREFIX}img${(idx % IMAGE_COUNT) + 1}.jpg")
                .buildUpon()
                .appendQueryParameter(PARAM_TYPE, mimeType)
                .appendQueryParameter(PARAM_SIZE_META, includeSize.toString())
                .appendQueryParameter("index", idx.toString())
                .build()

        fun getImageSize(uri: Uri): Size? {
            val name = uri.lastPathSegment ?: return null
            return sizeMap[name]
        }

        const val IMAGE_COUNT = 8

        const val URI_PREFIX = "content://com.android.sharetest.provider/"
        const val PARAM_TYPE = "type"
        const val PARAM_SIZE_META = "ismeta"
        val ICON_URI: Uri = Uri.parse("${URI_PREFIX}letter_a.png")

        var getTypeLatency = 0L
        var openLatency = 0L
        var openFailureRate = 0f
        var queryLatency = 0L
    }
}


