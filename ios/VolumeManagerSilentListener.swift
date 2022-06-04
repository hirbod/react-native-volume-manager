import Mute

@objc(VolumeManagerSilentListener)
class SilentListener: RCTEventEmitter {
    override static func requiresMainQueueSetup() -> Bool {
      return true;
    }
    
    override func supportedEvents() -> [String]! {
      return ["RNVMSilentEvent"]
    }
    
    var previousValue: Bool = false
    var hasListeners: Bool = false
    
    override init() {
      super.init()
      
      Mute.shared.checkInterval = 2.0
      Mute.shared.alwaysNotify = true
      
      Mute.shared.notify = onNotify
    }
    
    @objc func setInterval(_ newInterval: NSNumber) {
        Mute.shared.checkInterval = Double(newInterval);
    }
    
    func onNotify(_ newVal: Bool) {
      if previousValue == newVal {
        return
      }
      
      previousValue = newVal
      if hasListeners {
        sendEvent(withName: "RNVMSilentEvent", body: newVal)
      }
    }
    
    override func startObserving() {
      self.hasListeners = true
    }
    
    override func stopObserving() {
      self.hasListeners = false;
    }
}
