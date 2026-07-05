// File: MasterAppEngine.kt
package com.sudhansh.musicplayer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlin.math.abs

// 1. The Sudhansh Playback Service
class SudhanshPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}

// 2. The Main Controller & Gesture Interface
class MainActivity : AppCompatActivity() {
    
    private lateinit var player: ExoPlayer
    private lateinit var audioManager: AudioManager
    private lateinit var gestureDetector: GestureDetector

    private val noisyAudioReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                player.pause()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Load the new Flagship UI we just built
        setContentView(R.layout.activity_main)
        
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initializeSmartPlayer()
        registerReceiver(noisyAudioReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
        
        // Initialize Gesture Engine
        setupGestureControls()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureControls() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            
            // Fling detects fast swipes across the screen
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                
                // Horizontal Swipes
                if (abs(diffX) > abs(diffY) && abs(diffX) > 100 && abs(velocityX) > 100) {
                    if (diffX > 0) {
                        Toast.makeText(applicationContext, "Skipped to Previous", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(applicationContext, "Skipped to Next", Toast.LENGTH_SHORT).show()
                    }
                    return true
                } 
                // Vertical Swipes
                else if (abs(diffY) > 100 && abs(velocityY) > 100) {
                    if (diffY > 0) {
                        Toast.makeText(applicationContext, "Minimizing Player", Toast.LENGTH_SHORT).show()
                    }
                    return true
                }
                return false
            }

            // Double Tap detects Favourites
            override fun onDoubleTap(e: MotionEvent): Boolean {
                Toast.makeText(applicationContext, "Added to Sudhansh Favourites", Toast.LENGTH_SHORT).show()
                return true
            }
        })

        // Attach the gesture engine to the entire screen
        val rootView = findViewById<View>(R.id.mainPlayerRoot)
        rootView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun initializeSmartPlayer() {
        val qualityUrl = getOptimalStreamUrl()
        player = ExoPlayer.Builder(this).build()
        val mediaItem = MediaItem.fromUri(qualityUrl)
        player.setMediaItem(mediaItem)
        player.repeatMode = Player.REPEAT_MODE_ALL
    }

    private fun getOptimalStreamUrl(): String {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return "url_128kbps_aac"
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "url_128kbps_aac"

        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
            "url_320kbps_mp3"
        } else {
            "url_128kbps_aac" 
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(noisyAudioReceiver)
        player.release()
    }
}
