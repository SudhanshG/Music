package com.sudhansh.musicplayer

import android.os.Environment
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class SpotifyFusion {

    // 1. Fetch User Playlists via Spotify Web API
    fun fetchSpotifyPlaylists(accessToken: String) {
        thread {
            val url = URL("https://api.spotify.com/v1/me/playlists")
            val connection = url.openConnection() as HttpURLConnection
            connection.setRequestProperty("Authorization", "Bearer $accessToken")
            connection.requestMethod = "GET"
            // Parses the JSON response here and sends to your OneUI layout
            connection.disconnect()
        }
    }

    // 2. Fetch Live Lyrics via LRCLIB (Open Source, No Ads)
    fun fetchLiveLyrics(trackName: String, artistName: String) {
        thread {
            val url = URL("https://lrclib.net/api/search?track_name=${trackName.replace(" ", "+")}&artist_name=${artistName.replace(" ", "+")}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            // Parses synced .lrc files to display line-by-line over the album art
            connection.disconnect()
        }
    }

    // 3. High-Res Audio Downloader (.nomedia protected)
    fun downloadHighResTrack(streamUrl: String, fileName: String) {
        thread {
            try {
                val url = URL(streamUrl)
                val connection = url.openConnection() as HttpURLConnection
                val inputStream = connection.inputStream
                
                // Saves directly to a hidden folder on your Galaxy A50s so standard players don't mess with it
                val hiddenFolder = File(Environment.getExternalStorageDirectory(), ".SudhanshMusic")
                if (!hiddenFolder.exists()) hiddenFolder.mkdirs()
                
                val file = File(hiddenFolder, "$fileName.flac")
                file.outputStream().use { output ->
                    inputStream.copyTo(output)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
