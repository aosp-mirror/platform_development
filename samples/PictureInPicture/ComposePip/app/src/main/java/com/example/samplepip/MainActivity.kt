package com.example.samplepip

import android.app.PictureInPictureParams
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.media3.exoplayer.ExoPlayer
import com.example.samplepip.R.raw
import com.example.samplepip.ui.theme.PiPComposeSampleTheme


class MainActivity : ComponentActivity() {
  private var player: ExoPlayer? = null
  @RequiresApi(Build.VERSION_CODES.O)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    player = ExoPlayer.Builder(applicationContext).build()
    setContent {
      Column {
        PiPComposeSampleTheme {
          SampleVideoPlayer(
            videoUri = Uri.parse("android.resource://$packageName/${raw.samplevideo}"),
            modifier = Modifier.fillMaxWidth().pictureInPicture(onBoundsChange = ::onBoundsChange),
            player = player!!

            //TODO(b/276395464): only auto enter on unpaused state: add callback for playing and pausing
            // as well as onUserLeaveHint
          )
        }

        PipButton()
      }
    }
  }

  @RequiresApi(Build.VERSION_CODES.O)
  @Composable
  fun PipButton() {
    Button(onClick = {
      enterPictureInPictureMode()
    }) {
      Text(text = getString(R.string.start_pip_button))
    }
  }

  private fun onBoundsChange(bounds: Rect) {
    /**
     * Whenever the players bounds change, we want to update our params
     */
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val builder = PictureInPictureParams.Builder()
        .setSourceRectHint(bounds)
        .setAspectRatio(Rational(bounds.width(), bounds.height()))
      setPictureInPictureParams(builder.build())
    }
  }
}