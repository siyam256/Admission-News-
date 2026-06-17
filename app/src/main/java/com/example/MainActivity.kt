package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        networkMonitor = NetworkMonitor(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer(networkMonitor)
            }
        }
    }
}

@Composable
fun MainAppContainer(networkMonitor: NetworkMonitor) {
    var screenState by remember { mutableStateOf("SPLASH") }
    val isOnline by remember {
        networkMonitor.isOnlineFlow
    }.collectAsState(initial = networkMonitor.isCurrentlyConnected())

    // Tracks Web loading issues explicitly
    var isWebViewError by remember { mutableStateOf(false) }

    // If connectivity drops, immediately divert to the offline screen (unless on splash)
    LaunchedEffect(isOnline) {
        if (!isOnline && screenState != "SPLASH") {
            screenState = "OFFLINE"
        }
    }

    when (screenState) {
        "SPLASH" -> {
            SplashScreen(
                isOnline = isOnline,
                onTimeout = { online ->
                    if (online) {
                        screenState = "MAIN"
                    } else {
                        screenState = "OFFLINE"
                    }
                }
            )
        }
        "OFFLINE" -> {
            OfflineScreen(
                onRetry = {
                    val currentOnline = networkMonitor.isCurrentlyConnected()
                    if (currentOnline) {
                        isWebViewError = false
                        screenState = "MAIN"
                    }
                }
            )
        }
        "MAIN" -> {
            if (isWebViewError || !isOnline) {
                LaunchedEffect(Unit) {
                    screenState = "OFFLINE"
                }
            } else {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainWebWrapper(
                        url = "https://admission-calendar.com/",
                        isOnlineState = isOnline,
                        onErrorTriggered = {
                            isWebViewError = true
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun SplashScreen(isOnline: Boolean, onTimeout: (Boolean) -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulseLogo")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val fadeAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        fadeAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(800, easing = LinearOutSlowInEasing)
        )
        delay(2200) // Stay on splash to showcase branding/logo beautifully
        onTimeout(isOnline)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White), // Match white background precisely as requested
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(24.dp)
                .graphicsLayer {
                    alpha = fadeAlpha.value
                }
        ) {
            // Clean seamless logo display with pulse animation
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Admission Calendar Logo",
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            CircularProgressIndicator(
                color = Color(0xFFEF4444), // Brand accent color
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun OfflineScreen(onRetry: () -> Unit) {
    var isChecking by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEE2E2)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5)),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ইন্টারনেট সংযোগ নেই",
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color(0xFF991B1B),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (!isChecking) {
                            isChecking = true
                            coroutineScope.launch {
                                delay(600)
                                onRetry()
                                isChecking = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = "পুনরায় চেষ্টা করুন",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainWebWrapper(
    url: String,
    isOnlineState: Boolean,
    onErrorTriggered: () -> Unit,
    modifier: Modifier = Modifier
) {
    var loadingProgress by remember { mutableStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        WebContent(
            url = url,
            onProgressUpdate = { progress ->
                loadingProgress = progress
            },
            onErrorTriggered = onErrorTriggered
        )
        
        // Centered progress loader with clean overlay
        if (loadingProgress < 100) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF1D4ED8),
                    strokeWidth = 4.dp,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
fun WebContent(
    url: String,
    onProgressUpdate: (Int) -> Unit = {},
    onErrorTriggered: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var webView: WebView? by remember { mutableStateOf(null) }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressUpdate(newProgress)
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                        super.doUpdateVisitedHistory(view, url, isReload)
                        webView = view
                    }

                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val requestUrl = request?.url?.toString() ?: return false
                        if (requestUrl.isNotEmpty()) {
                            // Let the initial home URL load in the WebView, open any other link in default browser
                            val isHomeUrl = requestUrl == url || 
                                            requestUrl == "$url/" || 
                                            requestUrl == "${url}index.html" || 
                                            requestUrl == "${url}index.php" ||
                                            requestUrl.startsWith("https://admission-calendar.com/?")
                            
                            if (isHomeUrl) {
                                return false
                            }
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(requestUrl))
                                view?.context?.startActivity(intent)
                                return true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        return false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        if (errorCode == ERROR_HOST_LOOKUP || errorCode == ERROR_CONNECT || errorCode == ERROR_TIMEOUT) {
                            onErrorTriggered()
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        if (request?.isForMainFrame == true) {
                            onErrorTriggered()
                        }
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
