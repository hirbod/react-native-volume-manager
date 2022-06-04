#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

@interface RCT_EXTERN_MODULE(VolumeManagerSilentListener, RCTEventEmitter)
RCT_EXTERN_METHOD(setInterval: (nonnull NSNumber *) newInterval)

@end
