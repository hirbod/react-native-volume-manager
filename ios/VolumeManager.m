#import <AVFoundation/AVFoundation.h>
#import <VolumeManager.h>

@import MediaPlayer;
@import UIKit;

@implementation VolumeManager {
  bool hasListeners;
  MPVolumeView *volumeView;
  UISlider *volumeSlider;
}

- (void)dealloc {
  [self removeVolumeListener];
}

- (instancetype)init {
  self = [super init];
  if (self) {
    [self addVolumeListener];
  }

  [self initVolumeView];
  return self;
}

- (void)initVolumeView {
  volumeView = [[MPVolumeView alloc] init];
  [self showVolumeUI:YES];
  for (UIView *view in volumeView.subviews) {
    if ([view.class.description isEqualToString:@"MPVolumeSlider"]) {
      volumeSlider = (UISlider *)view;
      break;
    }
  }

  [[NSNotificationCenter defaultCenter] addObserver:self
                                           selector:@selector(applicationWillEnterForeground:)
                                               name:UIApplicationWillEnterForegroundNotification
                                             object:nil];
}

- (NSArray<NSString *> *)supportedEvents {
  return @[ @"RNVMEventVolume" ];
}

+ (BOOL)requiresMainQueueSetup {
  return YES;
}

RCT_EXPORT_MODULE(VolumeManager)

- (void)startObserving {
  hasListeners = YES;
}

- (void)stopObserving {
  hasListeners = NO;
}

- (void)showVolumeUI:(BOOL)flag {
  if (flag && [volumeView superview]) {
    [volumeView removeFromSuperview];
  } else if (!flag && ![volumeView superview]) {
    [[[[UIApplication sharedApplication] keyWindow] rootViewController].view addSubview:volumeView];
  }
}

- (void)addVolumeListener {
  AVAudioSession *audioSession = [AVAudioSession sharedInstance];
  [audioSession setCategory:AVAudioSessionCategoryAmbient
                withOptions:AVAudioSessionCategoryOptionMixWithOthers | AVAudioSessionCategoryOptionAllowBluetooth
                      error:nil];
  [audioSession setActive:YES error:nil];

  [audioSession addObserver:self
                 forKeyPath:@"outputVolume"
                    options:NSKeyValueObservingOptionNew | NSKeyValueObservingOptionOld
                    context:nil];
}

- (void)removeVolumeListener {
  AVAudioSession *audioSession = [AVAudioSession sharedInstance];
  [audioSession removeObserver:self forKeyPath:@"outputVolume"];
}

- (void)observeValueForKeyPath:(NSString *)keyPath
                      ofObject:(id)object
                        change:(NSDictionary<NSString *, id> *)change
                       context:(void *)context {
  if (object == [AVAudioSession sharedInstance] && [keyPath isEqualToString:@"outputVolume"]) {
    float newValue = [change[@"new"] floatValue];
    if (hasListeners) {
      [self sendEventWithName:@"RNVMEventVolume" body:@{@"volume" : [NSNumber numberWithFloat:newValue]}];
    }
  }
}

RCT_EXPORT_METHOD(showNativeVolumeUI : (NSDictionary *)showNativeVolumeUI) {
  dispatch_sync(dispatch_get_main_queue(), ^{
    id enabled = [showNativeVolumeUI objectForKey:@"enabled"];
    [self showVolumeUI:(enabled != nil && [enabled boolValue])];
  });
}

RCT_EXPORT_METHOD(setVolume : (float)val config : (NSDictionary *)config) {
  dispatch_sync(dispatch_get_main_queue(), ^{
    id showUI = [config objectForKey:@"showUI"];
    [self showVolumeUI:(showUI != nil && [showUI boolValue])];
    volumeSlider.value = val;
  });
}

RCT_EXPORT_METHOD(getVolume : (NSString *)type resolve : (RCTPromiseResolveBlock)resolve rejecter : (RCTPromiseRejectBlock)reject) {
  dispatch_sync(dispatch_get_main_queue(), ^{
    resolve([NSNumber numberWithFloat:[volumeSlider value]]);
  });
}

RCT_EXTERN_METHOD(setMuteListenerInterval : (nonnull NSNumber *)newInterval)

RCT_EXPORT_METHOD(enable : (BOOL)enabled async : (BOOL)async) {
  if (async) {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
      AVAudioSession *session = [AVAudioSession sharedInstance];
      [session setCategory:AVAudioSessionCategoryAmbient error:nil];
      [session setActive:enabled withOptions:AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation error:nil];
    });
  } else {
    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setCategory:AVAudioSessionCategoryAmbient error:nil];
    [session setActive:enabled withOptions:AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation error:nil];
  }
}

RCT_EXPORT_METHOD(setActive : (BOOL)active async : (BOOL)async) {
  if (async) {
    dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
      AVAudioSession *session = [AVAudioSession sharedInstance];
      [session setActive:active withOptions:AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation error:nil];
    });
  } else {
    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setActive:active withOptions:AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation error:nil];
  }
}

RCT_EXPORT_METHOD(setMode : (NSString *)modeName) {
  AVAudioSession *session = [AVAudioSession sharedInstance];
  NSString *mode = nil;

  if ([modeName isEqual:@"Default"]) {
    mode = AVAudioSessionModeDefault;
  } else if ([modeName isEqual:@"VoiceChat"]) {
    mode = AVAudioSessionModeVoiceChat;
  } else if ([modeName isEqual:@"VideoChat"]) {
    mode = AVAudioSessionModeVideoChat;
  } else if ([modeName isEqual:@"GameChat"]) {
    mode = AVAudioSessionModeGameChat;
  } else if ([modeName isEqual:@"VideoRecording"]) {
    mode = AVAudioSessionModeVideoRecording;
  } else if ([modeName isEqual:@"Measurement"]) {
    mode = AVAudioSessionModeMeasurement;
  } else if ([modeName isEqual:@"MoviePlayback"]) {
    mode = AVAudioSessionModeMoviePlayback;
  } else if ([modeName isEqual:@"SpokenAudio"]) {
    mode = AVAudioSessionModeSpokenAudio;
  }

  if (mode) {
    [session setMode:mode error:nil];
  }
}

RCT_EXPORT_METHOD(setCategory : (NSString *)categoryName mixWithOthers : (BOOL)mixWithOthers) {
  AVAudioSession *session = [AVAudioSession sharedInstance];
  NSString *category = nil;

  if ([categoryName isEqual:@"Ambient"]) {
    category = AVAudioSessionCategoryAmbient;
  } else if ([categoryName isEqual:@"SoloAmbient"]) {
    category = AVAudioSessionCategorySoloAmbient;
  } else if ([categoryName isEqual:@"Playback"]) {
    category = AVAudioSessionCategoryPlayback;
  } else if ([categoryName isEqual:@"Record"]) {
    category = AVAudioSessionCategoryRecord;
  } else if ([categoryName isEqual:@"PlayAndRecord"]) {
    category = AVAudioSessionCategoryPlayAndRecord;
  } else if ([categoryName isEqual:@"MultiRoute"]) {
    category = AVAudioSessionCategoryMultiRoute;
  }

  if (category) {
    if (mixWithOthers) {
      [session setCategory:category
               withOptions:AVAudioSessionCategoryOptionMixWithOthers | AVAudioSessionCategoryOptionAllowBluetooth
                     error:nil];
    } else {
      [session setCategory:category error:nil];
    }
  }
}

RCT_EXPORT_METHOD(enableInSilenceMode : (BOOL)enabled) {
  dispatch_async(dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setCategory:AVAudioSessionCategoryPlayback error:nil];
    [session setActive:enabled error:nil];
  });
}

- (void)applicationWillEnterForeground:(NSNotification *)notification {
  if (hasListeners) {
    AVAudioSession *audioSession = [AVAudioSession sharedInstance];
    [audioSession setActive:YES error:nil];
  }
}

@end
