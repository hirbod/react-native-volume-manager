package com.reactnativevolumemanager;

import static com.facebook.react.bridge.UiThreadUtil.runOnUiThread;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import android.app.Dialog;
import android.widget.FrameLayout;
import android.view.WindowManager;

@ReactModule(name = VolumeManagerModule.NAME)
public class VolumeManagerModule extends ReactContextBaseJavaModule implements ActivityEventListener, LifecycleEventListener {
  public static final String NAME = "VolumeManager";
  private final String TAG = VolumeManagerModule.class.getSimpleName();

  private static final String VOL_VOICE_CALL = "call";
  private static final String VOL_SYSTEM = "system";
  private static final String VOL_RING = "ring";
  private static final String VOL_MUSIC = "music";
  private static final String VOL_ALARM = "alarm";
  private static final String VOL_NOTIFICATION = "notification";
  private static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
  private static final String RNVM_EVENT_VOLUME = "RNVMEventVolume";

  private final ReactApplicationContext mContext;
  private final AudioManager am;
  private final VolumeBroadcastReceiver volumeBR;

  private Boolean showNativeVolumeUI = true;
  private Boolean hardwareButtonListenerRegistered = false;

  String category;

  public VolumeManagerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    mContext = reactContext;
    reactContext.addLifecycleEventListener(this);
    am = (AudioManager) mContext.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    volumeBR = new VolumeBroadcastReceiver();
    this.category = null;
  }

  private Dialog createTransparentDialog() {
    Activity currentActivity = mContext.getCurrentActivity();
    if (currentActivity == null) return null;

    Dialog transparentDialog = new Dialog(currentActivity, android.R.style.Theme_Translucent_NoTitleBar);
    transparentDialog.setContentView(new FrameLayout(currentActivity));
    transparentDialog.setCancelable(false);

    // Set FLAG_NOT_TOUCHABLE to allow touch events to pass through the dialog
    WindowManager.LayoutParams layoutParams = transparentDialog.getWindow().getAttributes();
    layoutParams.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
    transparentDialog.getWindow().setAttributes(layoutParams);

    return transparentDialog;
  }

  private void registerVolumeReceiver() {
    if (!volumeBR.isRegistered()) {
      IntentFilter filter = new IntentFilter(VOLUME_CHANGED_ACTION);
      mContext.registerReceiver(volumeBR, filter);
      volumeBR.setRegistered(true);
    }
  }

  private void unregisterVolumeReceiver() {
    if (volumeBR.isRegistered()) {
      try {
        mContext.unregisterReceiver(volumeBR);
        volumeBR.setRegistered(false);
      } catch (IllegalArgumentException e) {
        Log.w(TAG, "Error unregistering volume receiver", e);
      }
    }
  }

  private void setupKeyListener() {
    runOnUiThread(() -> {
      if (hardwareButtonListenerRegistered) return;
      Activity currentActivity = mContext.getCurrentActivity();
      if (currentActivity == null) return;

      Dialog transparentDialog = createTransparentDialog();
      if (transparentDialog == null) return;

      View rootView = transparentDialog.getWindow().getDecorView();
      rootView.setFocusableInTouchMode(true);
      rootView.requestFocus();
      rootView.setOnKeyListener((v, keyCode, event) -> {
        hardwareButtonListenerRegistered = true;

        if (showNativeVolumeUI) return false;

        switch (event.getKeyCode()) {
        case KeyEvent.KEYCODE_VOLUME_UP:
          am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_RAISE,
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
          return true;
        case KeyEvent.KEYCODE_VOLUME_DOWN:
          am.adjustStreamVolume(AudioManager.STREAM_MUSIC,
            AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
          return true;
        default:
          return false;
        }
      });

      transparentDialog.show();
    });
  }

  @ReactMethod
  public void showNativeVolumeUI(ReadableMap config) {
    if (config.hasKey("enabled")) {
      showNativeVolumeUI = config.getBoolean("enabled");
    }
    setupKeyListener();
  }

  @NonNull
  @Override
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void enable(final Boolean enabled) {
    // no op
  }

  @ReactMethod
  public void setCategory(final String category, final Boolean mixWithOthers) {
    this.category = category;
  }

  @ReactMethod
  public void getRingerMode(Promise promise) {
    int mode = am.getRingerMode();
    promise.resolve(mode);
  }

  private boolean hasDndAccess() {
    NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
    return (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) || nm.isNotificationPolicyAccessGranted();
  }

  @ReactMethod
  public void checkDndAccess(Promise promise) {
    promise.resolve(hasDndAccess());
  }

  @ReactMethod
  public void requestDndAccess(Promise promise) {
    if (!hasDndAccess() && mContext.hasCurrentActivity()) {
      Intent intent = null;
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
      }
      if (intent != null) {
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Context context = mContext.getCurrentActivity().getApplicationContext();
        context.startActivity(intent);
        promise.resolve(true);
      } else {
        promise.resolve(false);
      }
    } else {
      promise.resolve(false);
    }
  }

  @ReactMethod
  public void setRingerMode(int mode, Promise promise) {
    try {
      am.setRingerMode(mode);
      promise.resolve(mode);
    } catch (Exception err) {
      promise.reject(err);
    }
  }

  @ReactMethod
  public void setVolume(float val, ReadableMap config) {
    if (config == null) {
      Log.w(TAG, "Missing configuration for setVolume method.");
      return;
    }

    unregisterVolumeReceiver();
    String type = config.getString("type");
    boolean playSound = config.getBoolean("playSound");
    boolean showUI = config.getBoolean("showUI");

    if (type == null) {
      Log.w(TAG, "Volume type is null in setVolume method.");
      return;
    }

    int volType = getVolType(type);
    int flags = 0;
    if (playSound) {
      flags |= AudioManager.FLAG_PLAY_SOUND;
    }
    if (showUI) {
      flags |= AudioManager.FLAG_SHOW_UI;
    }
    try {
      am.setStreamVolume(volType, (int)(val * am.getStreamMaxVolume(volType)), flags);
    } catch (SecurityException e) {
      if (val == 0) {
        Log.w(TAG, "setVolume(0) failed. See https://github.com/c19354837/react-native-system-setting/issues/48");
        NotificationManager notificationManager =
          (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
          !notificationManager.isNotificationPolicyAccessGranted()) {
          Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
          mContext.startActivity(intent);
        }
      }
      Log.e(TAG, "err", e);
    }
    registerVolumeReceiver();
  }

  @ReactMethod
  public void getVolume(String type, Promise promise) {
    promise.resolve(getNormalizationVolume(type));
  }

  @ReactMethod
  public void addListener(String eventName) {
    // Keep
  }

  @ReactMethod
  public void removeListeners(Integer count) {
    // Keep
  }

  private int getVolType(String type) {
    int volType;
    switch (type) {
    case VOL_VOICE_CALL:
      volType = AudioManager.STREAM_VOICE_CALL;
      break;
    case VOL_SYSTEM:
      volType = AudioManager.STREAM_SYSTEM;
      break;
    case VOL_RING:
      volType = AudioManager.STREAM_RING;
      break;
    case VOL_MUSIC:
      volType = AudioManager.STREAM_MUSIC;
      break;
    case VOL_ALARM:
      volType = AudioManager.STREAM_ALARM;
      break;
    case VOL_NOTIFICATION:
      volType = AudioManager.STREAM_NOTIFICATION;
      break;
    default:
      volType = AudioManager.USE_DEFAULT_STREAM_TYPE;
      break;
    }
    return volType;
  }

  private float getNormalizationVolume(String type) {
    int volType = getVolType(type);
    int volume = am.getStreamVolume(volType);
    int maxVolume = am.getStreamMaxVolume(volType);
    return (float) volume / maxVolume;
  }

  @Override
  public void onHostResume() {
    registerVolumeReceiver();
    setupKeyListener();
  }

  @Override
  public void onHostPause() {
    unregisterVolumeReceiver();
  }

  @Override
  public void onHostDestroy() {
    unregisterVolumeReceiver();
  }

  @Override
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
    // Keep
  }

  @Override
  public void onNewIntent(Intent intent) {
    // Keep
  }

  private class VolumeBroadcastReceiver extends BroadcastReceiver {
    private boolean isRegistered = false;

    @Override
    public void onReceive(Context context, Intent intent) {
      if (VOLUME_CHANGED_ACTION.equals(intent.getAction())) {
        int type = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
        String typeName = null;
        switch (type) {
        case AudioManager.STREAM_VOICE_CALL:
          typeName = VOL_VOICE_CALL;
          break;
        case AudioManager.STREAM_SYSTEM:
          typeName = VOL_SYSTEM;
          break;
        case AudioManager.STREAM_RING:
          typeName = VOL_RING;
          break;
        case AudioManager.STREAM_MUSIC:
          typeName = VOL_MUSIC;
          break;
        case AudioManager.STREAM_ALARM:
          typeName = VOL_ALARM;
          break;
        case AudioManager.STREAM_NOTIFICATION:
          typeName = VOL_NOTIFICATION;
          break;
        }

        if (typeName != null && (category == null || category.equals(typeName))) {
          WritableMap data = Arguments.createMap();
          data.putString("type", typeName);
          data.putDouble("volume", getNormalizationVolume(typeName));
          mContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit(RNVM_EVENT_VOLUME, data);
        }
      }
    }

    public boolean isRegistered() {
      return isRegistered;
    }

    public void setRegistered(boolean registered) {
      isRegistered = registered;
    }
  }
}
