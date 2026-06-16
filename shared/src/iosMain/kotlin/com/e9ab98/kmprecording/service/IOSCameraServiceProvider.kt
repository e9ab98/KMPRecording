package com.e9ab98.kmprecording.service

object IOSCameraServiceProvider {
    private var _instance: IOSCameraService? = null

    val instance: IOSCameraService
        get() {
            if (_instance == null) {
                _instance = IOSCameraService()
            }
            return _instance!!
        }

    fun release() {
        _instance?.release()
        _instance = null
    }
}
