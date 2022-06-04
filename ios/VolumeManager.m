#import <VolumeManager.h>
#import <AVFoundation/AVFoundation.h>

@import MediaPlayer;
@import UIKit;

@implementation VolumeManager {
  bool hasListeners;
  long skipSetVolumeCount;
  MPVolumeView *volumeView;
  UISlider *volumeSlider;
}

-(void)dealloc {
    [self removeVolumeListener];
}

-(instancetype)init {
    self = [super init];
    if(self){
        [self addVolumeListener];
    }
  
    [self initVolumeView];
    return self;
}

-(void)initVolumeView {
    skipSetVolumeCount = 0;
    volumeView = [[MPVolumeView alloc] initWithFrame:CGRectMake(-[UIScreen mainScreen].bounds.size.width, 0, 0, 0)];
    [self showVolumeUI:YES];
    for (UIView* view in volumeView.subviews) {
        if ([view.class.description isEqualToString:@"MPVolumeSlider"]){
            volumeSlider = (UISlider*)view;
            break;
        }
    }
}

+(BOOL)requiresMainQueueSetup{
    return YES;
}

RCT_EXPORT_MODULE()

RCT_EXPORT_METHOD(setVolume:(float)val config:(NSDictionary *)config){
    skipSetVolumeCount++;
    dispatch_sync(dispatch_get_main_queue(), ^{
        id showUI = [config objectForKey:@"showUI"];
        [self showVolumeUI:(showUI != nil && [showUI boolValue])];
        volumeSlider.value = val;
    });
}

RCT_EXPORT_METHOD(getVolume:(NSString *)type resolve:(RCTPromiseResolveBlock)resolve rejecter:(RCTPromiseRejectBlock)reject){
    dispatch_sync(dispatch_get_main_queue(), ^{
        resolve([NSNumber numberWithFloat:[volumeSlider value]]);
    });
}

RCT_EXTERN_METHOD(setMuteListenerInterval: (nonnull NSNumber *) newInterval)

-(void)showVolumeUI:(BOOL)flag{
    if(flag && [volumeView superview]){
        [volumeView removeFromSuperview];
    }else if(!flag && ![volumeView superview]){
        [[[[UIApplication sharedApplication] keyWindow] rootViewController].view addSubview:volumeView];
    }
}

- (NSArray<NSString *> *)supportedEvents
{
    return @[@"RNVMEventVolume"];
}

-(void)startObserving {
    hasListeners = YES;
}

-(void)stopObserving {
    hasListeners = NO;
}

- (void)addVolumeListener {
        AVAudioSession* audioSession = [AVAudioSession sharedInstance];
        [audioSession setActive:YES error:nil];
        [audioSession addObserver:self
                       forKeyPath:@"outputVolume"
                          options:NSKeyValueObservingOptionNew | NSKeyValueObservingOptionOld
                          context:nil];
}

-(void)removeVolumeListener {
    AVAudioSession* audioSession = [AVAudioSession sharedInstance];
    [audioSession removeObserver:self forKeyPath:@"outputVolume"];
}

- (void)observeValueForKeyPath:(NSString *)keyPath ofObject:(id)object change:(NSDictionary<NSString *,id> *)change context:(void *)context {

    if (object == [AVAudioSession sharedInstance] && [keyPath isEqualToString:@"outputVolume"]) {
        float newValue = [change[@"new"] floatValue];
        if (skipSetVolumeCount == 0 && hasListeners) {
            [self sendEventWithName:@"EventVolume"
                               body:@{@"volume": [NSNumber numberWithFloat:newValue]}];
        }
        if (skipSetVolumeCount > 0) {
            skipSetVolumeCount--;
        }
    }
}

@end
