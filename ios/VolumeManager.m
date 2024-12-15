#import <AVFoundation/AVFoundation.h>
#import <VolumeManager.h>
#import <AVKit/AVRoutePickerView.h>

@import MediaPlayer;
@import UIKit;

@interface CustomVolumeView : UIView
@property(nonatomic, strong) UISlider *volumeSlider;
@property(nonatomic, strong) MPVolumeView *systemVolumeView;
@end

@implementation CustomVolumeView

- (instancetype)initWithFrame:(CGRect)frame {
  self = [super initWithFrame:frame];
  if (self) {
    self.translatesAutoresizingMaskIntoConstraints = NO;
    self.userInteractionEnabled = NO;
    [self setupSystemVolumeView];
    [self setupVolumeSlider];
  }
  return self;
}

- (void)setupSystemVolumeView {
  self.systemVolumeView = [[MPVolumeView alloc] initWithFrame:CGRectMake(-2000, -2000, 1, 1)];
  self.systemVolumeView.alpha = 0.01;
  [self addSubview:self.systemVolumeView];
}

- (void)setupVolumeSlider {
  MPVolumeView *volumeView = [[MPVolumeView alloc] initWithFrame:self.bounds];
  for (UIView *view in [volumeView subviews]) {
    if ([view isKindOfClass:[UISlider class]]) {
      self.volumeSlider = (UISlider *)view;
      break;
    }
  }
  
  if (self.volumeSlider) {
    [self addSubview:self.volumeSlider];
    self.volumeSlider.translatesAutoresizingMaskIntoConstraints = NO;
    self.volumeSlider.hidden = YES;
    
    [NSLayoutConstraint activateConstraints:@[
      [self.volumeSlider.leadingAnchor constraintEqualToAnchor:self.leadingAnchor constant:8],
      [self.volumeSlider.trailingAnchor constraintEqualToAnchor:self.trailingAnchor constant:-8],
      [self.volumeSlider.centerYAnchor constraintEqualToAnchor:self.centerYAnchor],
      [self.heightAnchor constraintEqualToConstant:44]
    ]];
  }
}

@end

@implementation VolumeManager {
  bool hasListeners;
  CustomVolumeView *customVolumeView;
  AVAudioSession *audioSession;
  MPVolumeView *hiddenVolumeView;
}

- (void)dealloc {
  [self removeVolumeListener];
}

- (instancetype)init {
  self = [super init];
  if (self) {
    audioSession = [AVAudioSession sharedInstance];
    [self addVolumeListener];
  }
  [self initVolumeView];
  return self;
}

- (UIWindow *)getAppropriateWindow {
  UIWindowScene *windowScene = nil;
  NSSet<UIScene *> *connectedScenes = [[UIApplication sharedApplication] connectedScenes];
  
  for (UIScene *scene in connectedScenes) {
    if ([scene isKindOfClass:[UIWindowScene class]] &&
        scene.activationState == UISceneActivationStateForegroundActive) {
      windowScene = (UIWindowScene *)scene;
      break;
    }
  }
  
  if (!windowScene) {
    return nil;
  }
  
  // Look for key window in the active scene
  for (UIWindow *window in windowScene.windows) {
    if (window.isKeyWindow) {
      return window;
    }
  }
  
  // Fallback to first window if no key window found
  return windowScene.windows.firstObject;
}

- (void)initVolumeView {
  hiddenVolumeView = [[MPVolumeView alloc] initWithFrame:CGRectMake(-2000, -2000, 1, 1)];
  hiddenVolumeView.alpha = 0.01;
  
  UIWindow *window = [self getAppropriateWindow];
  [window addSubview:hiddenVolumeView];
  
  customVolumeView = [[CustomVolumeView alloc] initWithFrame:CGRectZero];
  [self showVolumeUI:YES];
  
  [[NSNotificationCenter defaultCenter]
   addObserver:self
   selector:@selector(applicationWillEnterForeground:)
   name:UIApplicationWillEnterForegroundNotification
   object:nil];
}

- (UIViewController *)topMostViewController {
  UIWindow *window = [self getAppropriateWindow];
  UIViewController *topController = window.rootViewController;
  
  while (topController.presentedViewController) {
    topController = topController.presentedViewController;
  }
  return topController;
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
  __weak typeof(self) weakSelf = self;
  dispatch_async(dispatch_get_main_queue(), ^{
    __strong typeof(weakSelf) strongSelf = weakSelf;
    if (strongSelf) {
      if (!flag) {
        if (![strongSelf->hiddenVolumeView superview]) {
          UIWindow *window = [strongSelf getAppropriateWindow];
          [window addSubview:strongSelf->hiddenVolumeView];
        }
        strongSelf->customVolumeView.volumeSlider.hidden = YES;
      } else {
        [strongSelf->hiddenVolumeView removeFromSuperview];
        strongSelf->customVolumeView.volumeSlider.hidden = NO;
      }
    }
  });
}

- (void)addVolumeListener {
  [audioSession setCategory:AVAudioSessionCategoryAmbient
                withOptions:AVAudioSessionCategoryOptionMixWithOthers |
   AVAudioSessionCategoryOptionAllowBluetooth
                      error:nil];
  [audioSession setActive:YES error:nil];
  
  [audioSession addObserver:self
                 forKeyPath:@"outputVolume"
                    options:NSKeyValueObservingOptionNew | NSKeyValueObservingOptionOld
                    context:nil];
}

- (void)removeVolumeListener {
  [audioSession removeObserver:self forKeyPath:@"outputVolume"];
}

- (void)observeValueForKeyPath:(NSString *)keyPath
                      ofObject:(id)object
                        change:(NSDictionary<NSString *, id> *)change
                       context:(void *)context {
  if (object == [AVAudioSession sharedInstance] &&
      [keyPath isEqualToString:@"outputVolume"]) {
    float newValue = [change[@"new"] floatValue];
    if (hasListeners) {
      [self sendEventWithName:@"RNVMEventVolume"
                         body:@{@"volume" : [NSNumber numberWithFloat:newValue]}];
    }
  }
}

RCT_EXPORT_METHOD(showNativeVolumeUI:(NSDictionary *)showNativeVolumeUI) {
  __weak typeof(self) weakSelf = self;
  dispatch_async(dispatch_get_main_queue(), ^{
    __strong typeof(weakSelf) strongSelf = weakSelf;
    if (strongSelf) {
      id enabled = [showNativeVolumeUI objectForKey:@"enabled"];
      [strongSelf showVolumeUI:(enabled != nil && [enabled boolValue])];
    }
  });
}

RCT_EXPORT_METHOD(setVolume:(float)val config:(NSDictionary *)config) {
  __weak typeof(self) weakSelf = self;
  dispatch_async(dispatch_get_main_queue(), ^{
    __strong typeof(weakSelf) strongSelf = weakSelf;
    if (strongSelf) {
      id showUI = [config objectForKey:@"showUI"];
      [strongSelf showVolumeUI:(showUI != nil && [showUI boolValue])];
      strongSelf->customVolumeView.volumeSlider.value = val;
    }
  });
}

RCT_EXPORT_METHOD(getVolume:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  __weak typeof(self) weakSelf = self;
  dispatch_async(dispatch_get_main_queue(), ^{
    __strong typeof(weakSelf) strongSelf = weakSelf;
    if (strongSelf) {
      NSNumber *volumeNumber = [NSNumber
                                numberWithFloat:[strongSelf->customVolumeView.volumeSlider value]];
      NSDictionary *volumeDictionary = @{@"volume" : volumeNumber};
      resolve(volumeDictionary);
    }
  });
}

RCT_EXPORT_METHOD(enable:(BOOL)enabled async:(BOOL)async) {
  if (async) {
    dispatch_async(
                   dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
                     AVAudioSession *session = [AVAudioSession sharedInstance];
                     [session setCategory:AVAudioSessionCategoryAmbient error:nil];
                     [session setActive:enabled
                            withOptions:AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
                                  error:nil];
                   });
  } else {
    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setCategory:AVAudioSessionCategoryAmbient error:nil];
    [session setActive:enabled
           withOptions:AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
                 error:nil];
  }
}

RCT_EXPORT_METHOD(setActive:(BOOL)active async:(BOOL)async) {
  if (async) {
    dispatch_async(
                   dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
                     AVAudioSession *session = [AVAudioSession sharedInstance];
                     [session setActive:active
                            withOptions:AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
                                  error:nil];
                   });
  } else {
    AVAudioSession *session = [AVAudioSession sharedInstance];
    [session setActive:active
           withOptions:AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation
                 error:nil];
  }
}

RCT_EXPORT_METHOD(setMode:(NSString *)modeName) {
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

RCT_EXPORT_METHOD(setCategory:(NSString *)categoryName
                  mixWithOthers:(BOOL)mixWithOthers) {
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
               withOptions:AVAudioSessionCategoryOptionMixWithOthers |
       AVAudioSessionCategoryOptionAllowBluetooth
                     error:nil];
    } else {
      [session setCategory:category error:nil];
    }
  }
}

RCT_EXPORT_METHOD(enableInSilenceMode:(BOOL)enabled) {
  dispatch_async(
                 dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_DEFAULT, 0), ^{
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
