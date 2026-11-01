package com.academialife.app

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private val splashDurationMs = 3600L // 3.6 seconds (within 3-5 sec range)

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val frameLogoCard = findViewById<FrameLayout>(R.id.frameLogoCard)
        val tvSplashBrand = findViewById<TextView>(R.id.tvSplashBrand)
        val tvSplashTagline = findViewById<TextView>(R.id.tvSplashTagline)
        val layoutGreetingPill = findViewById<LinearLayout>(R.id.layoutGreetingPill)
        val viewGlowRingOuter = findViewById<View>(R.id.viewGlowRingOuter)
        val viewGlowRingInner = findViewById<View>(R.id.viewGlowRingInner)

        // Initial states for punchy animations
        frameLogoCard.scaleX = 0.15f
        frameLogoCard.scaleY = 0.15f
        frameLogoCard.alpha = 0f

        viewGlowRingOuter.scaleX = 0.2f
        viewGlowRingOuter.scaleY = 0.2f
        viewGlowRingOuter.alpha = 0f

        viewGlowRingInner.scaleX = 0.2f
        viewGlowRingInner.scaleY = 0.2f
        viewGlowRingInner.alpha = 0f

        // 1. Play "Kya Haal Hai Dost" Hindi Voice Greeting
        playGreetingSound()

        // 2. Animate Logo expanding / zooming with the screen (Neumorphic Punch)
        frameLogoCard.animate()
            .scaleX(1.05f)
            .scaleY(1.05f)
            .alpha(1.0f)
            .setDuration(1600)
            .setInterpolator(OvershootInterpolator(1.4f))
            .withEndAction {
                // Subtle breathing / settling bounce
                frameLogoCard.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(400)
                    .start()
            }
            .start()

        // 3. Animate Glow Rings
        viewGlowRingOuter.animate()
            .scaleX(1.6f)
            .scaleY(1.6f)
            .alpha(0.35f)
            .setDuration(2000)
            .setInterpolator(DecelerateInterpolator())
            .start()

        viewGlowRingInner.animate()
            .scaleX(1.25f)
            .scaleY(1.25f)
            .alpha(0.6f)
            .setDuration(1800)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 4. Animate Brand Name & Tagline fade-in
        tvSplashBrand.translationY = 30f
        tvSplashBrand.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(600)
            .start()

        tvSplashTagline.translationY = 20f
        tvSplashTagline.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(800)
            .setStartDelay(900)
            .start()

        // 5. Animate Greeting Pill pop-in
        layoutGreetingPill.scaleX = 0.5f
        layoutGreetingPill.scaleY = 0.5f
        layoutGreetingPill.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1f)
            .setDuration(700)
            .setStartDelay(1300)
            .setInterpolator(OvershootInterpolator(1.6f))
            .start()

        // 6. Preload AI analysis and authentication in background while animation plays
        lifecycleScope.launch {
            FirebaseRepository.ensureAuthenticated()
            StudentAIEngine.runProactiveAnalysis()
        }

        // 7. Transition to Main Screen after ~3.6s
        Handler(Looper.getMainLooper()).postDelayed({
            proceedToNextScreen()
        }, splashDurationMs)
    }

    private fun playGreetingSound() {
        try {
            val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
            // Ensure media volume is loud and audible for the student greeting
            val maxVol = audioManager?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: 15
            val currentVol = audioManager?.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) ?: 0
            if (currentVol < (maxVol * 0.75).toInt()) {
                audioManager?.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, (maxVol * 0.85).toInt(), 0)
            }

            mediaPlayer = MediaPlayer.create(this, R.raw.kya_haal_hai_dost)
            mediaPlayer?.setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()
            )
            mediaPlayer?.setVolume(1.0f, 1.0f)
            mediaPlayer?.setOnCompletionListener { mp ->
                mp.release()
                mediaPlayer = null
            }
            mediaPlayer?.start()
        } catch (_: Exception) {
            // Audio fallback gracefully handled
        }
    }

    private fun proceedToNextScreen() {
        val intent = if (FirebaseRepository.isUserSignedIn()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

