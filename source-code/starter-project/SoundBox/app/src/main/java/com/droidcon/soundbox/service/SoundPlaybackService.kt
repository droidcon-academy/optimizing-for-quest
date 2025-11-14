package com.droidcon.soundbox.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.droidcon.soundbox.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SoundPlaybackService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration = _duration.asStateFlow()

    inner class LocalBinder : Binder() {
        fun getService(): SoundPlaybackService = this@SoundPlaybackService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val soundName = intent?.getStringExtra(EXTRA_SOUND_NAME) ?: "Playing Sound"
        startForeground(NOTIFICATION_ID, createNotification(soundName))
        return START_NOT_STICKY
    }

    fun playSound(soundResId: Int?) {
        if (soundResId == null) return
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, soundResId).apply {
            setOnPreparedListener(::onMediaPlayerPrepared)
            setOnCompletionListener(::onMediaPlayerCompletion)
        }
    }

    fun playSound(filePath: String?) {
        if (filePath == null) return
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(filePath)
            setOnPreparedListener(::onMediaPlayerPrepared)
            setOnCompletionListener(::onMediaPlayerCompletion)
            prepareAsync()
        }
    }

    private fun onMediaPlayerPrepared(mp: MediaPlayer) {
        _duration.value = mp.duration
        mp.start()
        _isPlaying.value = true
        startUpdatingPosition()
    }

    private fun onMediaPlayerCompletion(mp: MediaPlayer) {
        _isPlaying.value = false
        _currentPosition.value = 0
    }

    fun togglePlayPause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            } else {
                it.start()
                _isPlaying.value = true
                startUpdatingPosition()
            }
        }
    }

    fun seekTo(position: Float) {
        mediaPlayer?.seekTo((position * _duration.value).toInt())
    }

    private fun startUpdatingPosition() {
        serviceScope.launch {
            while (_isPlaying.value) {
                mediaPlayer?.let { _currentPosition.value = it.currentPosition }
                delay(100)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, "Sound Playback",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun createNotification(soundName: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Playing: $soundName")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "SoundPlaybackServiceChannel"
        const val EXTRA_SOUND_NAME = "extra_sound_name"
    }
}