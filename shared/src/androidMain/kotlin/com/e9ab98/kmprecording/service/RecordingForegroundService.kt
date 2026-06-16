package com.e9ab98.kmprecording.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class RecordingForegroundService : Service() {
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    private var recordingDuration = 0
    private var isPaused = false
    private var isLoopMode = false
    private var segmentIndex = 0
    
    private val _isServiceRecording = MutableStateFlow(false)
    val isServiceRecording: StateFlow<Boolean> = _isServiceRecording
    
    private val binder = LocalBinder()
    
    inner class LocalBinder : Binder() {
        fun getService(): RecordingForegroundService = this@RecordingForegroundService
    }
    
    private var mainActivityClassName: String? = null
    
    companion object {
        const val CHANNEL_ID = "recording_channel"
        const val NOTIFICATION_ID = 1001
        
        const val ACTION_START_RECORDING = "start_recording"
        const val ACTION_STOP_RECORDING = "stop_recording"
        const val ACTION_UPDATE_DURATION = "update_duration"
        const val ACTION_UPDATE_PAUSED = "update_paused"
        const val ACTION_UPDATE_LOOP_MODE = "update_loop_mode"
        const val ACTION_UPDATE_SEGMENT = "update_segment"
        
        const val EXTRA_DURATION = "duration"
        const val EXTRA_IS_PAUSED = "is_paused"
        const val EXTRA_IS_LOOP_MODE = "is_loop_mode"
        const val EXTRA_SEGMENT_INDEX = "segment_index"
        const val EXTRA_ACTIVITY_CLASS = "activity_class"
        
        private var notificationIconResId: Int = 0
        private var activityClassName: String? = null
        
        fun init(context: Context, activityClass: Class<*>) {
            notificationIconResId = context.resources.getIdentifier(
                "ic_launcher_foreground",
                "mipmap",
                context.packageName
            )
            if (notificationIconResId == 0) {
                notificationIconResId = context.resources.getIdentifier(
                    "ic_launcher",
                    "mipmap",
                    context.packageName
                )
            }
            if (notificationIconResId == 0) {
                notificationIconResId = android.R.drawable.ic_menu_camera
            }
            activityClassName = activityClass.name
        }
        
        fun start(context: Context) {
            if (notificationIconResId == 0) {
                init(context, getMainActivityClass(context))
            }
            val intent = Intent(context, RecordingForegroundService::class.java)
            intent.action = ACTION_START_RECORDING
            intent.putExtra(EXTRA_ACTIVITY_CLASS, activityClassName)
            context.startForegroundService(intent)
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, RecordingForegroundService::class.java)
            intent.action = ACTION_STOP_RECORDING
            context.startService(intent)
        }
        
        fun updateDuration(context: Context, duration: Int) {
            val intent = Intent(context, RecordingForegroundService::class.java)
            intent.action = ACTION_UPDATE_DURATION
            intent.putExtra(EXTRA_DURATION, duration)
            context.startService(intent)
        }
        
        fun updatePaused(context: Context, isPaused: Boolean) {
            val intent = Intent(context, RecordingForegroundService::class.java)
            intent.action = ACTION_UPDATE_PAUSED
            intent.putExtra(EXTRA_IS_PAUSED, isPaused)
            context.startService(intent)
        }
        
        fun updateLoopMode(context: Context, isLoopMode: Boolean, segmentIndex: Int = 0) {
            val intent = Intent(context, RecordingForegroundService::class.java)
            intent.action = ACTION_UPDATE_LOOP_MODE
            intent.putExtra(EXTRA_IS_LOOP_MODE, isLoopMode)
            intent.putExtra(EXTRA_SEGMENT_INDEX, segmentIndex)
            context.startService(intent)
        }
        
        private fun getMainActivityClass(context: Context): Class<*> {
            val packageName = context.packageName
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
                setPackage(packageName)
            }
            val resolveInfo = context.packageManager.resolveActivity(intent, 0)
            val className = resolveInfo?.activityInfo?.name ?: "$packageName.MainActivity"
            return Class.forName(className)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                mainActivityClassName = intent.getStringExtra(EXTRA_ACTIVITY_CLASS) ?: activityClassName
                _isServiceRecording.value = true
                startForeground(NOTIFICATION_ID, createNotification())
            }
            ACTION_STOP_RECORDING -> {
                _isServiceRecording.value = false
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_UPDATE_DURATION -> {
                recordingDuration = intent.getIntExtra(EXTRA_DURATION, 0)
                updateNotification()
            }
            ACTION_UPDATE_PAUSED -> {
                isPaused = intent.getBooleanExtra(EXTRA_IS_PAUSED, false)
                updateNotification()
            }
            ACTION_UPDATE_LOOP_MODE -> {
                isLoopMode = intent.getBooleanExtra(EXTRA_IS_LOOP_MODE, false)
                segmentIndex = intent.getIntExtra(EXTRA_SEGMENT_INDEX, 0)
                updateNotification()
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        _isServiceRecording.value = false
        serviceScope.cancel()
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "视频录制",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "视频录制服务通知"
        channel.setShowBadge(false)
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(): Notification {
        val activityClass = try {
            Class.forName(mainActivityClassName ?: activityClassName ?: "${packageName}.MainActivity")
        } catch (e: Exception) {
            null
        }
        
        val pendingIntent = if (activityClass != null) {
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, activityClass).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }
        
        val title = when {
            isPaused -> "录制已暂停"
            isLoopMode -> "循环录制中 #${segmentIndex + 1}"
            else -> "正在录制视频"
        }
        
        val content = when {
            isPaused -> "点击返回应用继续录制"
            else -> formatDuration(recordingDuration)
        }
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(notificationIconResId)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
        
        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }
        
        return builder.build()
    }
    
    private fun updateNotification() {
        if (_isServiceRecording.value) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }
    }
    
    private fun formatDuration(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, secs)
    }
}