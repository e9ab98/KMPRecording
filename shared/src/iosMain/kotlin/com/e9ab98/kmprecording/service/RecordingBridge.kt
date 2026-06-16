package com.e9ab98.kmprecording.service

import platform.AVFoundation.AVCaptureMovieFileOutput
import platform.AVFoundation.AVCaptureSession
import platform.Foundation.NSURL
import platform.UIKit.UIView
import platform.darwin.NSObject

object RecordingBridge {
    var startRecording: ((AVCaptureMovieFileOutput, NSURL, NSObject) -> Unit)? = null
    var createPreviewView: (() -> UIView)? = null
    var setPreviewSession: ((UIView, AVCaptureSession) -> Unit)? = null
    var createPlayerView: (() -> UIView)? = null
    var setPlayerVideoURL: ((UIView, NSURL) -> Unit)? = null
    var playerPlay: ((UIView) -> Unit)? = null
    var playerPause: ((UIView) -> Unit)? = null
    var playerCleanup: ((UIView) -> Unit)? = null
}
