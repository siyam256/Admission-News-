package com.example

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          WebContent(
            url = "https://admission-calendar.com/",
            modifier = Modifier.padding(innerPadding)
          )
        }
      }
    }
  }
}

@Composable
fun WebContent(url: String, modifier: Modifier = Modifier) {
  var webView: WebView? by remember { mutableStateOf(null) }

  // Handle back button navigation inside WebView
  BackHandler(enabled = webView?.canGoBack() == true) {
    webView?.goBack()
  }

  AndroidView(
    factory = { context ->
      WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true // Enable DOM Storage for modern Javascript/Web databases
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        
        webViewClient = object : WebViewClient() {
          override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            // Trigger state change so that BackHandler can update its enabled status
            webView = view
          }
        }
        loadUrl(url)
        webView = this
      }
    },
    update = { view ->
      webView = view
    },
    modifier = modifier.fillMaxSize()
  )
}
