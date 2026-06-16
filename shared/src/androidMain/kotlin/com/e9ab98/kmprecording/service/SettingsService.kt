package com.e9ab98.kmprecording.service

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.e9ab98.kmprecording.model.RecordingConfig
import com.e9ab98.kmprecording.model.Resolution
import com.e9ab98.kmprecording.model.VideoQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "recording_settings")

class SettingsService(private val context: Context) {
    
    private object Keys {
        val RESOLUTION = intPreferencesKey("resolution")
        val VIDEO_QUALITY = intPreferencesKey("video_quality")
        val ENABLE_AUDIO = booleanPreferencesKey("enable_audio")
        val SEGMENT_DURATION = intPreferencesKey("segment_duration")
        val MAX_STORAGE_MB = longPreferencesKey("max_storage_mb")
        val LOOP_MODE_DEFAULT = booleanPreferencesKey("loop_mode_default")
        val APP_LANGUAGE = intPreferencesKey("app_language")
        val SHOW_HUD = booleanPreferencesKey("show_hud")
        val GENERATE_SRT = booleanPreferencesKey("generate_srt")
    }
    
    val recordingConfig: Flow<RecordingConfig> = context.dataStore.data.map { prefs ->
        RecordingConfig(
            resolution = Resolution.entries.getOrElse(prefs[Keys.RESOLUTION] ?: 1) { Resolution.FHD_1080P },
            quality = VideoQuality.entries.getOrElse(prefs[Keys.VIDEO_QUALITY] ?: 2) { VideoQuality.HIGH },
            enableAudio = prefs[Keys.ENABLE_AUDIO] ?: true,
            segmentDurationSeconds = prefs[Keys.SEGMENT_DURATION] ?: 300,
            maxStorageMB = prefs[Keys.MAX_STORAGE_MB] ?: 1024 * 1024,
            showHud = prefs[Keys.SHOW_HUD] ?: true,
            generateSrt = prefs[Keys.GENERATE_SRT] ?: true
        )
    }
    
    val loopModeDefault: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.LOOP_MODE_DEFAULT] ?: false
    }
    
    suspend fun updateResolution(resolution: Resolution) {
        context.dataStore.edit { prefs ->
            prefs[Keys.RESOLUTION] = Resolution.entries.indexOf(resolution)
        }
    }
    
    suspend fun updateVideoQuality(quality: VideoQuality) {
        context.dataStore.edit { prefs ->
            prefs[Keys.VIDEO_QUALITY] = VideoQuality.entries.indexOf(quality)
        }
    }
    
    suspend fun updateEnableAudio(enable: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ENABLE_AUDIO] = enable
        }
    }
    
    suspend fun updateSegmentDuration(durationSeconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SEGMENT_DURATION] = durationSeconds
        }
    }
    
    suspend fun updateMaxStorageMB(maxMB: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MAX_STORAGE_MB] = maxMB
        }
    }
    
    suspend fun updateLoopModeDefault(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOOP_MODE_DEFAULT] = enabled
        }
    }

    val appLanguage: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.APP_LANGUAGE] ?: 0
    }

    suspend fun updateAppLanguage(langIndex: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_LANGUAGE] = langIndex
        }
    }

    suspend fun updateShowHud(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SHOW_HUD] = enabled
        }
    }

    suspend fun updateGenerateSrt(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GENERATE_SRT] = enabled
        }
    }
}