package com.example.samplepip

import android.graphics.Rect
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toRect
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun SampleVideoPlayer(
  videoUri: Uri,
  modifier: Modifier,
  player: ExoPlayer,
) {
  AndroidView(
    factory = {
      PlayerView(it).apply {
        //TODO(b/276395464): set proper player functionality
        setPlayer(player)
        player.setMediaItem(MediaItem.fromUri(videoUri))
        player.play()
      }
    },
    modifier = modifier,
  )
}

// callback to get player bounds, used to smoothly enter PIP
fun Modifier.pictureInPicture(onBoundsChange: (Rect) -> Unit): Modifier {
  return this then Modifier.onGloballyPositioned { layoutCoordinates ->
    // Use onGloballyPositioned to get player bounds, which gets passed back to the main activity
    val sourceRect = layoutCoordinates.boundsInWindow().toAndroidRectF().toRect()
    onBoundsChange(sourceRect)
  }
}