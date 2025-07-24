/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.ai.edge.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.os.bundleOf
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.ai.edge.gallery.ui.theme.GalleryTheme
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.ai.edge.gallery.sms.SmsAnalysisActivity
import com.google.ai.edge.gallery.sms.SmsReceiver
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    installSplashScreen()

    enableEdgeToEdge()
    // Fix for three-button nav not properly going edge-to-edge.
    // See: https://issuetracker.google.com/issues/298296168
    window.isNavigationBarContrastEnforced = false
    setContent { GalleryTheme { Surface(modifier = Modifier.fillMaxSize()) { GalleryApp() } } }
    // Keep the screen on while the app is running for better demo experience.
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    
    // Request SMS permissions
    requestSmsPermissions()
  }

  override fun onResume() {
    super.onResume()

    firebaseAnalytics?.logEvent(
      FirebaseAnalytics.Event.APP_OPEN,
      bundleOf(
        "app_version" to "1.0.4",
        "os_version" to Build.VERSION.SDK_INT.toString(),
        "device_model" to Build.MODEL,
      ),
    )
    
    // Test SMS functionality - long press on screen to trigger test
    val rootView = findViewById<android.view.View>(android.R.id.content)
    rootView.setOnLongClickListener {
      // Test broadcast receiver
      val testIntent = Intent("com.google.ai.edge.gallery.TEST_SMS").apply {
        putExtra("sms_sender", "+1234567890")
        putExtra("sms_body", "Test SMS from long press")
      }
      sendBroadcast(testIntent)
      Log.d("MainActivity", "Test broadcast sent from long press")
      true
    }
    
    // Log receiver status
    SmsReceiver.logReceiverStatus(this)
  }
  
  private fun requestSmsPermissions() {
    val permissions = arrayOf(
      Manifest.permission.RECEIVE_SMS,
      Manifest.permission.READ_SMS
    )
    
    val permissionsToRequest = mutableListOf<String>()
    
    for (permission in permissions) {
      if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(permission)
      }
    }
    
    if (permissionsToRequest.isNotEmpty()) {
      Log.d("MainActivity", "Requesting SMS permissions: $permissionsToRequest")
      ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1001)
    } else {
      Log.d("MainActivity", "SMS permissions already granted")
    }
  }
  
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    
    if (requestCode == 1001) {
      val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
      Log.d("MainActivity", "SMS permissions result: $allGranted")
      
      if (allGranted) {
        Log.d("MainActivity", "All SMS permissions granted - Broadcast Receiver should work now")
      } else {
        Log.w("MainActivity", "Some SMS permissions denied - Broadcast Receiver may not work")
      }
    }
  }


}
