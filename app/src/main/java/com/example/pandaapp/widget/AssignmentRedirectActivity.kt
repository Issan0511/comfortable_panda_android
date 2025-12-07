package com.example.pandaapp.widget

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity

/**
 * Widget のタップから受け取り、ブラウザで目的の URL を開く中継用 Activity。
 */
class AssignmentRedirectActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent?.dataString
        if (url.isNullOrBlank()) {
            Log.e("AssignmentRedirect", "No URL found in intent")
            finish()
            return
        }

        try {
            val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            startActivity(viewIntent)
        } catch (e: ActivityNotFoundException) {
            Log.e("AssignmentRedirect", "No activity found to handle URL: $url", e)
        } finally {
            finish()
        }
    }
}
