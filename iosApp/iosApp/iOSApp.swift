import UIKit
import Shared
import AVFoundation

@main
class iOSApp: UIResponder, UIApplicationDelegate {
    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        RecordingHelper.setupBridge()

        window = UIWindow(frame: UIScreen.main.bounds)
        let rootViewController = MainViewControllerKt.MainViewController()
        rootViewController.view.frame = UIScreen.main.bounds
        window?.rootViewController = rootViewController
        window?.makeKeyAndVisible()

        // 使用 iOS 16+ 的新 API 设置横屏方向
        DispatchQueue.main.async {
            if let windowScene = self.window?.windowScene {
                windowScene.requestGeometryUpdate(.iOS(interfaceOrientations: .landscape))
            }
        }

        return true
    }

    func application(_ application: UIApplication, supportedInterfaceOrientationsFor window: UIWindow?) -> UIInterfaceOrientationMask {
        return .landscape
    }
}
