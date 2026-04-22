package com.lixiangyang.talky.ui.common

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.LruCache
import android.widget.ImageView
import androidx.core.net.toUri
import com.lixiangyang.talky.R
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

object VideoThumbnailLoader {
    private val memoryCache = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 16 / 1024).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val executor = Executors.newFixedThreadPool(2)

    fun loadInto(imageView: ImageView, videoPath: String, thumbnailPath: String) {
        val cacheKey = thumbnailPath.ifBlank { videoPath }
        imageView.tag = cacheKey

        memoryCache.get(cacheKey)?.let { bitmap ->
            imageView.setImageBitmap(bitmap)
            return
        }

        imageView.setImageResource(R.drawable.bg_video_thumb)
        executor.execute {
            val bitmap = loadBitmap(imageView.context, videoPath, thumbnailPath) ?: return@execute
            memoryCache.put(cacheKey, bitmap)
            imageView.post {
                if (imageView.tag == cacheKey) {
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    fun generateThumbnailFile(context: Context, videoPath: String): String {
        val bitmap = createVideoFrame(context, videoPath) ?: return ""
        val thumbnailDir = File(context.cacheDir, "video_thumbnails").apply { mkdirs() }
        val fileName = "thumb_${videoPath.hashCode()}.jpg"
        val target = File(thumbnailDir, fileName)
        FileOutputStream(target).use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        }
        memoryCache.put(target.absolutePath, bitmap)
        return target.absolutePath
    }

    private fun loadBitmap(context: Context, videoPath: String, thumbnailPath: String): Bitmap? {
        if (thumbnailPath.isNotBlank()) {
            val stored = decodeStoredThumbnail(thumbnailPath)
            if (stored != null) {
                return stored
            }
        }
        return createVideoFrame(context, videoPath)
    }

    private fun decodeStoredThumbnail(thumbnailPath: String): Bitmap? {
        return runCatching {
            when {
                thumbnailPath.startsWith("content://") -> null
                else -> BitmapFactory.decodeFile(thumbnailPath)
            }
        }.getOrNull()
    }

    private fun createVideoFrame(context: Context, videoPath: String): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return runCatching {
            if (videoPath.startsWith("content://")) {
                retriever.setDataSource(context, videoPath.toUri())
            } else {
                retriever.setDataSource(videoPath.removePrefix("file://"))
            }
            retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }.getOrNull().also {
            runCatching { retriever.release() }
        }
    }
}
