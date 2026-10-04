package com.taqni.mac
import android.annotation.SuppressLint
import android.app.Activity
import android.app.UiModeManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
  lateinit var web: WebView
  private val http = OkHttpClient.Builder().followRedirects(true).followSslRedirects(true)
    .connectTimeout(20, TimeUnit.SECONDS).readTimeout(40, TimeUnit.SECONDS).build()

  inner class Bridge {
    @JavascriptInterface fun sha(t: String): String =
      MessageDigest.getInstance("SHA-256").digest(t.toByteArray()).joinToString("") { "%02x".format(it) }

    @JavascriptInterface fun fetch(id: Int, url: String, headers: String) {
      Thread {
        var status = 0; var body = ""
        try {
          val rb = Request.Builder().url(url)
          val h = JSONObject(headers)
          h.keys().forEach { rb.header(it, h.getString(it)) }
          http.newCall(rb.build()).execute().use { r -> status = r.code; body = r.body?.string() ?: "" }
        } catch (e: Exception) { body = "ERR:" + (e.message ?: e.javaClass.simpleName) }
        val js = "__nf($id,$status,${JSONObject.quote(body)})"
        runOnUiThread { web.evaluateJavascript(js, null) }
      }.start()
    }

    @JavascriptInterface fun play(url: String, title: String, mac: String, live: Boolean) {
      runOnUiThread {
        startActivity(Intent(this@MainActivity, PlayerActivity::class.java)
          .putExtra("u", url).putExtra("t", title).putExtra("m", mac).putExtra("l", live))
      }
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  override fun onCreate(b: Bundle?) {
    super.onCreate(b)
    web = WebView(this); setContentView(web)
    web.settings.javaScriptEnabled = true
    web.settings.domStorageEnabled = true
    web.isFocusable = true
    web.isFocusableInTouchMode = true
    WebView.setWebContentsDebuggingEnabled(true)
    web.addJavascriptInterface(Bridge(), "Native")
    web.webChromeClient = WebChromeClient()

    val tv = (getSystemService(UI_MODE_SERVICE) as UiModeManager).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
      !packageManager.hasSystemFeature("android.hardware.touchscreen")
    val tvJs = if (tv) "localStorage.setItem('ms_tvauto','1');" else ""
    val err = "window.addEventListener('error',function(e){var d=document.createElement('div');d.style.cssText='position:fixed;top:0;left:0;right:0;z-index:999999;background:#900;color:#fff;font:12px monospace;padding:6px;direction:ltr';d.textContent='JS: '+e.message+' @'+e.lineno;(document.body||document.documentElement).appendChild(d)});"
    val head = "<script>" + err + "try{" + tvJs + "var c=JSON.parse(localStorage.getItem('ms_cfg')||'{}');if(!c.w){c.w='https://native.local';c.k='native';localStorage.setItem('ms_cfg',JSON.stringify(c))}}catch(e){}</script>"

    val html = assets.open("index.html").bufferedReader().readText()
    val shim = assets.open("shim.js").bufferedReader().readText()
    val out = html.replace("<head>", "<head>" + head).replace("</body>", "<script>" + shim + "</script></body>")
    web.loadDataWithBaseURL("https://app.local/", out, "text/html", "UTF-8", null)
    web.requestFocus()
  }

  override fun onBackPressed() { if (web.canGoBack()) web.goBack() else finish() }
}
