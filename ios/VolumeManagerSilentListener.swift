import Mute

@objc(VolumeManagerSilentListener)
class SilentListener: RCTEventEmitter {
  override static func requiresMainQueueSetup() -> Bool {
    return true
  }

  override func supportedEvents() -> [String]! {
    return ["RNVMSilentEvent"]
  }

  var previousValue: Bool = false
  var hasListeners: Bool = false
  var initialValueReported: Bool = false

  override init() {
    super.init()

    Mute.shared.checkInterval = 2.0
    Mute.shared.alwaysNotify = true
    Mute.shared.isPaused = true
    Mute.shared.notify = onNotify
  }

  @objc func setInterval(_ newInterval: NSNumber) {
    Mute.shared.checkInterval = Double(newInterval)
  }

  func onNotify(_ newVal: Bool) {
    if hasListeners {
      if previousValue == newVal && self.initialValueReported {
        return
      }
      let body = ["isMuted": newVal, "initialQuery": !self.initialValueReported]

      self.initialValueReported = true
      previousValue = newVal
      sendEvent(withName: "RNVMSilentEvent", body: body)
    }
  }

  override func startObserving() {
    self.hasListeners = true
    Mute.shared.isPaused = false
  }

  override func stopObserving() {
    self.hasListeners = false
    Mute.shared.isPaused = true
  }
}
