package com.e9ab98.kmprecording.domain

sealed class L10n {
    abstract val settingsTitle: String
    abstract val settingsButton: String
    abstract val videoResolution: String
    abstract val videoQuality: String
    abstract val audioRecording: String
    abstract val audioRecordingEnabled: String
    abstract val audioRecordingDisabled: String
    abstract val segmentDuration: String
    abstract val minutes: String
    abstract val maxStorage: String
    abstract val loopRecordingMode: String
    abstract val loopRecordingEnabledDesc: String
    abstract val loopRecordingDisabledDesc: String
    abstract val done: String
    abstract val backToRecord: String
    
    abstract val qualityLow: String
    abstract val qualityMedium: String
    abstract val qualityHigh: String
    abstract val qualityVeryHigh: String
    
    abstract val storageTitle: String
    abstract val storageDesc: String
    abstract val statisticsTitle: String
    abstract val statisticsDesc: String
    abstract val noVideos: String
    abstract val startRecording: String
    abstract val deleteTitle: String
    abstract val deleteConfirmMsg: String
    abstract val deleteButton: String
    abstract val cancelButton: String
    abstract val loading: String
    
    abstract val hourText: String
    abstract val minuteText: String
    abstract val secondText: String
    
    abstract val stateIdle: String
    abstract val statePreparing: String
    abstract val stateSaving: String
    abstract val statePaused: String
    abstract val stateError: String
    
    abstract val gallery: String
    abstract val switchCamera: String
    abstract val languageSetting: String
    abstract val languageOptionZh: String
    abstract val languageOptionEn: String
    
    abstract val modeLoop: String
    abstract val modeNormal: String
    abstract val cameraRear: String
    abstract val cameraFront: String
    abstract val loadVideosFailed: String
    abstract val deleteVideoFailed: String
    abstract val hudDisplay: String
    abstract val hudDisplayDesc: String
    abstract val generateSrtSubtitles: String
    abstract val generateSrtSubtitlesDesc: String

    companion object {
        fun get(lang: AppLanguage): L10n = when (lang) {
            AppLanguage.ZH -> ZhL10n
            AppLanguage.EN -> EnL10n
        }
    }
}

object ZhL10n : L10n() {
    override val settingsTitle = "录制设置"
    override val settingsButton = "设置"
    override val videoResolution = "视频分辨率"
    override val videoQuality = "视频质量"
    override val audioRecording = "音频录制"
    override val audioRecordingEnabled = "开启音频录制"
    override val audioRecordingDisabled = "仅录制视频"
    override val segmentDuration = "分段时长"
    override val minutes = "分钟"
    override val maxStorage = "最大存储空间"
    override val loopRecordingMode = "循环录制模式"
    override val loopRecordingEnabledDesc = "启动时默认循环分段"
    override val loopRecordingDisabledDesc = "启动时默认普通录制"
    override val done = "完成"
    override val backToRecord = "返回录制"
    
    override val qualityLow = "低 (2 Mbps)"
    override val qualityMedium = "中 (5 Mbps)"
    override val qualityHigh = "高 (10 Mbps)"
    override val qualityVeryHigh = "超高 (20 Mbps)"
    
    override val storageTitle = "存储空间"
    override val storageDesc = "已用 %sMB / 可用 %sMB"
    override val statisticsTitle = "统计信息"
    override val statisticsDesc = "%s 个视频 • %s"
    override val noVideos = "暂无视频"
    override val startRecording = "开始录制"
    override val deleteTitle = "删除视频"
    override val deleteConfirmMsg = "确定要删除这个视频吗？此操作无法撤销。"
    override val deleteButton = "删除"
    override val cancelButton = "取消"
    override val loading = "加载中"
    
    override val hourText = "时"
    override val minuteText = "分"
    override val secondText = "秒"
    
    override val stateIdle = "未录影"
    override val statePreparing = "准备中"
    override val stateSaving = "正在保存"
    override val statePaused = "已暂停"
    override val stateError = "录制错误"
    
    override val gallery = "图库"
    override val switchCamera = "翻转"
    override val languageSetting = "语言设置 / Language"
    override val languageOptionZh = "中文"
    override val languageOptionEn = "English"

    override val modeLoop = "循环"
    override val modeNormal = "普通"
    override val cameraRear = "后置"
    override val cameraFront = "前置"
    override val loadVideosFailed = "加载视频失败"
    override val deleteVideoFailed = "删除视频失败"
    override val hudDisplay = "HUD 仪表盘"
    override val hudDisplayDesc = "在录制界面上叠加显示行驶速度与 GPS 经纬度"
    override val generateSrtSubtitles = "生成字幕文件"
    override val generateSrtSubtitlesDesc = "录制时同步在同目录生成秒级 GPS 与速度数据的 .srt 字幕"
}

object EnL10n : L10n() {
    override val settingsTitle = "Recording Settings"
    override val settingsButton = "Settings"
    override val videoResolution = "Video Resolution"
    override val videoQuality = "Video Quality"
    override val audioRecording = "Audio Recording"
    override val audioRecordingEnabled = "Enable audio recording"
    override val audioRecordingDisabled = "Video only"
    override val segmentDuration = "Segment Duration"
    override val minutes = "min"
    override val maxStorage = "Max Storage Space"
    override val loopRecordingMode = "Loop Recording Mode"
    override val loopRecordingEnabledDesc = "Default to loop recording on startup"
    override val loopRecordingDisabledDesc = "Default to normal recording on startup"
    override val done = "Done"
    override val backToRecord = "Back to Record"
    
    override val qualityLow = "Low (2 Mbps)"
    override val qualityMedium = "Medium (5 Mbps)"
    override val qualityHigh = "High (10 Mbps)"
    override val qualityVeryHigh = "Very High (20 Mbps)"
    
    override val storageTitle = "Storage"
    override val storageDesc = "Used %sMB / Available %sMB"
    override val statisticsTitle = "Statistics"
    override val statisticsDesc = "%s videos • %s"
    override val noVideos = "No Videos"
    override val startRecording = "Start Recording"
    override val deleteTitle = "Delete Video"
    override val deleteConfirmMsg = "Are you sure you want to delete this video? This action cannot be undone."
    override val deleteButton = "Delete"
    override val cancelButton = "Cancel"
    override val loading = "Loading..."
    
    override val hourText = "h "
    override val minuteText = "m "
    override val secondText = "s"
    
    override val stateIdle = "Not Recording"
    override val statePreparing = "Preparing"
    override val stateSaving = "Saving"
    override val statePaused = "PAUSED"
    override val stateError = "Recording Error"
    
    override val gallery = "Gallery"
    override val switchCamera = "Switch"
    override val languageSetting = "Language / 语言"
    override val languageOptionZh = "中文 (Chinese)"
    override val languageOptionEn = "English"

    override val modeLoop = "Loop"
    override val modeNormal = "Norm"
    override val cameraRear = "Rear"
    override val cameraFront = "Front"
    override val loadVideosFailed = "Failed to load videos"
    override val deleteVideoFailed = "Failed to delete video"
    override val hudDisplay = "HUD Speedometer"
    override val hudDisplayDesc = "Overlay real-time speed and GPS coordinates on the recording screen"
    override val generateSrtSubtitles = "Generate SRT Subtitles"
    override val generateSrtSubtitlesDesc = "Generate synchronized .srt files containing GPS and speed data for each video segment"
}
