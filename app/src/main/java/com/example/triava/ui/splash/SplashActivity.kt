package com.example.triava.ui.splash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.triava.MainActivity
import com.example.triava.R
import com.example.triava.ui.intro.IntroActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoContainer = findViewById<LinearLayout>(R.id.logo_container)

        // Fade-in animation for logo
        val fadeIn = AlphaAnimation(0f, 1f).apply {
            interpolator = DecelerateInterpolator()
            duration = 1000
        }
        logoContainer.startAnimation(fadeIn)

        // Navigate after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            checkOnboardingAndNavigate()
        }, 2000)
    }

    private fun checkOnboardingAndNavigate() {
        val sharedPreferences = getSharedPreferences("BiblePlayPrefs", Context.MODE_PRIVATE)
        val isFirstTime = sharedPreferences.getBoolean("is_first_time", true)

        val intent = if (isFirstTime) {
            Intent(this, IntroActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }
        startActivity(intent)
        finish()
    }
}
