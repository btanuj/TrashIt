package com.example.trasher

import android.annotation.TargetApi
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val banner = TextView(this).apply {
            text = "No internet connection, loading local copy"
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 16)
            setBackgroundColor(0xFF333333.toInt())
            setTextColor(0xFFAAAAAA.toInt())
            visibility = android.view.View.GONE
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        root.addView(banner)
        root.addView(webView)
        setContentView(root)

        if (isOnline()) {
            webView.webViewClient = object : WebViewClient() {
                private fun showFallback(view: WebView) {
                    banner.visibility = android.view.View.VISIBLE
                    view.loadUrl("file:///android_asset/trash_schedule.html")
                }

                @TargetApi(Build.VERSION_CODES.M)
                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) showFallback(view)
                }

                @Suppress("DEPRECATION")
                override fun onReceivedError(view: WebView, errorCode: Int, description: String, failingUrl: String) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) showFallback(view)
                }
            }
            webView.loadUrl("https://btanuj.github.io/trasher/")
        } else {
            banner.visibility = android.view.View.VISIBLE
            webView.loadUrl("file:///android_asset/trash_schedule.html")
        }
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}