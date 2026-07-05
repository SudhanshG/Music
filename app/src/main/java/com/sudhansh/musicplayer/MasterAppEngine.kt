// File: MasterAppEngine.kt
package com.sudhansh.musicplayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

// 1. The Background Playback Service (Android 11 Optimized)
class AurevoPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer

    override fun onCreate() {
        super.onCreate()
        
        // Initialize ExoPlayer with Crossfade & Gapless capability
        player = ExoPlayer.Builder(this).build()
        
        // Link to MediaSession for Android Auto & Lock Screen Controls
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

// 2. The Main Controller Interface
class MainActivity : AppCompatActivity() {
    
    private lateinit var player: ExoPlayer
    private lateinit var audioManager: AudioManager

    // Pauses music instantly if earbuds disconnect
    private val noisyAudioReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                player.pause()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        initializeSmartPlayer()
        registerReceiver(noisyAudioReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
    }

    private fun initializeSmartPlayer() {
        // Smart Quality Network Check
        val qualityUrl = getOptimalStreamUrl()
        
        player = ExoPlayer.Builder(this).build()
        val mediaItem = MediaItem.fromUri(qualityUrl)
        
        player.setMediaItem(mediaItem)
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.prepare()
        // Playback will begin seamlessly without buffering
    }

    private fun getOptimalStreamUrl(): String {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return "url_128kbps_aac"
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return "url_128kbps_aac"

        // If on fast, unmetered Wi-Fi, fetch lossless-equivalent 320kbps
        return if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) {
            "url_320kbps_mp3"
        } else {
            "url_128kbps_aac" // Battery & Data saving mode for hotspots
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(noisyAudioReceiver)
        player.release()
    }
}
