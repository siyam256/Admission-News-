package com.example

import android.app.Application
import android.util.Log
import com.onesignal.OneSignal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val appId = BuildConfig.ONESIGNAL_APP_ID
        if (appId.isNotEmpty() && appId != "YOUR_ONESIGNAL_APP_ID") {
            try {
                // Initialize OneSignal
                OneSignal.initWithContext(this, appId)
                Log.d("MyApplication", "OneSignal initialized successfully with App ID: $appId")
                
                // Prompt for notification permissions on startup
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        OneSignal.Notifications.requestPermission(true)
                    } catch (e: Exception) {
                        Log.e("MyApplication", "Failed to request notification permission: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("MyApplication", "Failed to initialize OneSignal: ${e.message}")
            }
        } else {
            Log.w("MyApplication", "OneSignal APP_ID is not configured or set to placeholder. Please set ONESIGNAL_APP_ID in AI Studio Secrets.")
        }
    }
}
