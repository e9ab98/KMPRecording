import Foundation
import AVFoundation
import Shared
import UIKit

// PreviewView: 重写 layerClass 让 AVCaptureVideoPreviewLayer 成为视图的主 layer
@objc
public class PreviewView: UIView {
    override public class var layerClass: AnyClass {
        return AVCaptureVideoPreviewLayer.self
    }

    public var previewLayer: AVCaptureVideoPreviewLayer {
        return self.layer as! AVCaptureVideoPreviewLayer
    }
}

// PlayerView: 重写 layerClass 让 AVPlayerLayer 成为视图的主 layer
@objc
public class PlayerView: UIView {
    override public class var layerClass: AnyClass {
        return AVPlayerLayer.self
    }

    public var playerLayer: AVPlayerLayer {
        return self.layer as! AVPlayerLayer
    }

    private var player: AVPlayer?

    public func setVideoURL(_ url: URL) {
        print("PlayerView: setVideoURL called with \(url)")
        let playerItem = AVPlayerItem(url: url)
        player = AVPlayer(playerItem: playerItem)
        playerLayer.player = player
        playerLayer.videoGravity = .resizeAspect
        print("PlayerView: player set on layer, playerLayer.frame = \(playerLayer.frame)")
    }

    public func play() {
        print("PlayerView: play called")
        player?.play()
    }

    public func pause() {
        player?.pause()
    }

    public func cleanup() {
        player?.pause()
        player = nil
        playerLayer.player = nil
    }
}

@objc
public class RecordingHelper: NSObject {
    @objc public static func setupBridge() {
        // 录制桥接
        RecordingBridge.shared.startRecording = { output, url, delegate in
            if let connection = output.connection(with: .video), connection.isVideoOrientationSupported {
                connection.videoOrientation = .landscapeRight
            }
            output.startRecording(to: url as URL, recordingDelegate: delegate as! AVCaptureFileOutputRecordingDelegate)
        }

        // 预览视图桥接
        RecordingBridge.shared.createPreviewView = {
            let view = PreviewView()
            view.previewLayer.videoGravity = AVLayerVideoGravity.resizeAspectFill
            view.backgroundColor = UIColor.black
            return view
        }

        RecordingBridge.shared.setPreviewSession = { view, session in
            if let previewView = view as? PreviewView {
                previewView.previewLayer.session = session
                if let connection = previewView.previewLayer.connection {
                    if connection.isVideoOrientationSupported {
                        connection.videoOrientation = .landscapeRight
                    }
                }
            }
        }

        // 视频播放桥接
        RecordingBridge.shared.createPlayerView = {
            let view = PlayerView()
            view.backgroundColor = UIColor.black
            return view
        }

        RecordingBridge.shared.setPlayerVideoURL = { view, url in
            print("Bridge: setPlayerVideoURL called, view type = \(type(of: view))")
            if let playerView = view as? PlayerView {
                playerView.setVideoURL(url as URL)
            } else {
                print("Bridge: ERROR - view is not PlayerView!")
            }
        }

        RecordingBridge.shared.playerPlay = { view in
            if let playerView = view as? PlayerView {
                playerView.play()
            }
        }

        RecordingBridge.shared.playerPause = { view in
            if let playerView = view as? PlayerView {
                playerView.pause()
            }
        }

        RecordingBridge.shared.playerCleanup = { view in
            if let playerView = view as? PlayerView {
                playerView.cleanup()
            }
        }
    }
}
