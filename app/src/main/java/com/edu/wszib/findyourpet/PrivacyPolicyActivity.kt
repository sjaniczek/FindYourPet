package com.edu.wszib.findyourpet

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PrivacyPolicyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

        val webView = findViewById<WebView>(R.id.webViewPrivacyPolicy)
        webView.webViewClient = WebViewClient() // otwiera linki w WebView, nie w przeglądarce
        webView.settings.javaScriptEnabled = false // wyłącz JS, jeśli nie potrzebujesz

        // Wczytanie pliku z assets
        webView.loadUrl("file:///android_asset/privacy_policy.html")
    }
}