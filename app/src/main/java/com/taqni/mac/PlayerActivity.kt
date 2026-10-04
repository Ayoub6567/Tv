package com.taqni.mac
import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

@UnstableApi
class PlayerActivity : Activity() {
  private var player: ExoPlayer? = null
  private var idx = 0
  private lateinit var view: PlayerView
  private val uas = listOf(
    "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) MAG200 stbapp ver: 2 rev: 250 Safari/533.3",
    "VLC/3.0.18 LibVLC/3.0.18", "Lavf/60.3.100")

  override fun onCreate(b: Bundle?) {
    super.onCreate(b)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    view = PlayerView(this); setContentView(view)
    start()
  }

  private fun start() {
    player?.release()
    val url = intent.getStringExtra("u") ?: return
    val mac = intent.getStringExtra("m") ?: ""
    val h = HashMap<String, String>()
    if (idx == 0) { h["X-User-Agent"] = "Model: MAG250; Link: WiFi"; h["Cookie"] = "mac=$mac; stb_lang=en; timezone=GMT" }
    val ds = DefaultHttpDataSource.Factory().setUserAgent(uas[idx]).setAllowCrossProtocolRedirects(true)
      .setConnectTimeoutMs(15000).setReadTimeoutMs(20000).setDefaultRequestProperties(h)
    val p = ExoPlayer.Builder(this).setMediaSourceFactory(DefaultMediaSourceFactory(ds)).build()
    p.addListener(object : Player.Listener {
      override fun onPlayerError(e: PlaybackException) {
        if (idx < uas.size - 1) { idx++; start() }
        else Toast.makeText(this@PlayerActivity, "تعذّر التشغيل: " + e.errorCodeName + " " + (e.cause?.message ?: ""), Toast.LENGTH_LONG).show()
      }
    })
    view.player = p; player = p
    p.setMediaItem(MediaItem.fromUri(url)); p.prepare(); p.playWhenReady = true
  }

  override fun onKeyDown(k: Int, e: KeyEvent): Boolean {
    val p = player ?: return super.onKeyDown(k, e)
    val live = intent.getBooleanExtra("l", false)
    when (k) {
      KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { p.playWhenReady = !p.playWhenReady; return true }
      KeyEvent.KEYCODE_DPAD_RIGHT -> if (!live) { p.seekTo(p.currentPosition + 10000); return true }
      KeyEvent.KEYCODE_DPAD_LEFT -> if (!live) { p.seekTo(maxOf(0, p.currentPosition - 10000)); return true }
    }
    return super.onKeyDown(k, e)
  }

  override fun onDestroy() { player?.release(); super.onDestroy() }
}
