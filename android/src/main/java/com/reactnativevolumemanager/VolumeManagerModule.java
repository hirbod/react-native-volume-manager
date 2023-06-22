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

@ReactModule(name = VolumeManagerModule.NAME)
public class VolumeManagerModule
  extends ReactContextBaseJavaModule
  implements ActivityEventListener, LifecycleEventListener {

  public static final String NAME = "VolumeManager";
  private final String TAG = VolumeManagerModule.class.getSimpleName();

  private static final String VOL_VOICE_CALL = "call";
  private static final String VOL_SYSTEM = "system";
  private static final String VOL_RING = "ring";
  private static final String VOL_MUSIC = "music";
  private static final String VOL_ALARM = "alarm";
  private static final String VOL_NOTIFICATION = "notification";

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
    am =
      (AudioManager) mContext
        .getApplicationContext()
        .getSystemService(Context.AUDIO_SERVICE);
    volumeBR = new VolumeBroadcastReceiver();
    this.category = null;
  }

  private void registerVolumeReceiver() {
    if (!volumeBR.isRegistered()) {
      IntentFilter filter = new IntentFilter(
        "android.media.VOLUME_CHANGED_ACTION"
      );
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
        Log.e(TAG, "Error unregistering volume receiver", e);
      }
    }
  }

  private void setupKeyListener() {
    runOnUiThread(() -> {
      View rootView =
        ((ViewGroup) mContext.getCurrentActivity().getWindow().getDecorView());
      rootView.setFocusableInTouchMode(true);
      rootView.requestFocus();

      if (hardwareButtonListenerRegistered) return;

      rootView.setOnKeyListener((v, keyCode, event) -> {
        hardwareButtonListenerRegistered = true;
        if (showNativeVolumeUI) return false;

        switch (event.getKeyCode()) {
          case KeyEvent.KEYCODE_VOLUME_UP:
            am.adjustStreamVolume(
              AudioManager.STREAM_MUSIC,
              AudioManager.ADJUST_RAISE,
              AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
            );
            return true;
          case KeyEvent.KEYCODE_VOLUME_DOWN:
            am.adjustStreamVolume(
              AudioManager.STREAM_MUSIC,
              AudioManager.ADJUST_LOWER,
              AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE
            );
            return true;
          default:
            return false;
        }
      });
    });
  }

  @ReactMethod
  public void showNativeVolumeUI(ReadableMap config) {
    showNativeVolumeUI = config.getBoolean("enabled");
    // we want to listen to the hardware volume key buttons
    setupKeyListener();
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void enable(final Boolean enabled) {
    // no op
  }

  @ReactMethod
  public void setCategory(final String category, final Boolean mixWithOthers) {
    // no op
  }

  @ReactMethod
  public void getRingerMode(Promise promise) {
    int mode = am.getRingerMode();
    promise.resolve(mode);
  }

  private boolean hasDndAccess() {
    NotificationManager nm = (NotificationManager) mContext.getSystemService(
      Context.NOTIFICATION_SERVICE
    );
    return (
      (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) ||
      nm.isNotificationPolicyAccessGranted()
    );
  }

  @ReactMethod
  public void checkDndAccess(Promise promise) {
    promise.resolve(hasDndAccess());
  }

  @ReactMethod
  public void requestDndAccess(Promise promise) {
    if (!hasDndAccess() && mContext.hasCurrentActivity()) {
      Intent intent = null;
      if (
        android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M
      ) {
        intent =
          new Intent(
            android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
          );
      }
      assert intent != null;
      intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

      Context context = mContext.getCurrentActivity().getApplicationContext();
      context.startActivity(intent);
      promise.resolve(true);
    }

    promise.resolve(false);
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
    unregisterVolumeReceiver();
    String type = config.getString("type");
    boolean playSound = config.getBoolean("playSound");
    boolean showUI = config.getBoolean("showUI");
    assert type != null;
    int volType = getVolType(type);
    int flags = 0;
    if (playSound) {
      flags |= AudioManager.FLAG_PLAY_SOUND;
    }
    if (showUI) {
      flags |= AudioManager.FLAG_SHOW_UI;
    }
    try {
      am.setStreamVolume(
        volType,
        (int) (val * am.getStreamMaxVolume(volType)),
        flags
      );
    } catch (SecurityException e) {
      if (val == 0) {
        Log.w(
          TAG,
          "setVolume(0) failed. See https://github.com/c19354837/react-native-system-setting/issues/48"
        );
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(
          Context.NOTIFICATION_SERVICE
        );
        if (
          Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
          !notificationManager.isNotificationPolicyAccessGranted()
        ) {
          Intent intent = new Intent(
            android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
          );
          mContext.startActivity(intent);
        }
      }
      Log.e(TAG, "err", e);
    }
    registerVolumeReceiver();
  }

  @ReactMethod
  public void getVolume(Promise promise) {
    WritableMap result = Arguments.createMap();
    result.putDouble("volume", getNormalizationVolume(VOL_MUSIC));
    result.putDouble(VOL_VOICE_CALL, getNormalizationVolume(VOL_VOICE_CALL));
    result.putDouble(VOL_SYSTEM, getNormalizationVolume(VOL_SYSTEM));
    result.putDouble(VOL_RING, getNormalizationVolume(VOL_RING));
    result.putDouble(VOL_MUSIC, getNormalizationVolume(VOL_MUSIC));
    result.putDouble(VOL_ALARM, getNormalizationVolume(VOL_ALARM));
    result.putDouble(
      VOL_NOTIFICATION,
      getNormalizationVolume(VOL_NOTIFICATION)
    );
    promise.resolve(result);
  }

  @ReactMethod
  public void addListener(String eventName) {
    // Keep: Required for RN built in Event Emitter Calls.
  }

  @ReactMethod
  public void removeListeners(int count) {
    // Keep: Required for RN built in Event Emitter Calls.
  }

  private float getNormalizationVolume(String type) {
    int volType = getVolType(type);
    return am.getStreamVolume(volType) * 1.0f / am.getStreamMaxVolume(volType);
  }

  private int getVolType(String type) {
    switch (type) {
      case VOL_VOICE_CALL:
        return AudioManager.STREAM_VOICE_CALL;
      case VOL_SYSTEM:
        return AudioManager.STREAM_SYSTEM;
      case VOL_RING:
        return AudioManager.STREAM_RING;
      case VOL_ALARM:
        return AudioManager.STREAM_ALARM;
      case VOL_NOTIFICATION:
        return AudioManager.STREAM_NOTIFICATION;
      default:
        return AudioManager.STREAM_MUSIC;
    }
  }

  private void cleanupKeyListener() {
    runOnUiThread(() -> {
      if (!hardwareButtonListenerRegistered) return;
      View rootView =
        ((ViewGroup) mContext.getCurrentActivity().getWindow().getDecorView());
      rootView.setOnKeyListener(null);
      hardwareButtonListenerRegistered = false;
    });
  }

  @Override
  public void onActivityResult(
    Activity activity,
    int requestCode,
    int resultCode,
    Intent data
  ) {}

  @Override
  public void onNewIntent(Intent intent) {
    //
  }

  @Override
  public void onHostResume() {
    setupKeyListener();
    registerVolumeReceiver();
  }

  @Override
  public void onHostPause() {
    unregisterVolumeReceiver();
    cleanupKeyListener(); // Release the key listener when the screen loses focus
  }

  @Override
  public void onHostDestroy() {
    cleanupKeyListener(); // Release the key listener when the root view is destroyed
  }

  private class VolumeBroadcastReceiver extends BroadcastReceiver {

    private boolean isRegistered = false;

    public void setRegistered(boolean registered) {
      isRegistered = registered;
    }

    public boolean isRegistered() {
      return isRegistered;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
      if (intent.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {
        WritableMap para = Arguments.createMap();
        para.putDouble("volume", getNormalizationVolume(VOL_MUSIC));
        para.putDouble(VOL_VOICE_CALL, getNormalizationVolume(VOL_VOICE_CALL));
        para.putDouble(VOL_SYSTEM, getNormalizationVolume(VOL_SYSTEM));
        para.putDouble(VOL_RING, getNormalizationVolume(VOL_RING));
        para.putDouble(VOL_MUSIC, getNormalizationVolume(VOL_MUSIC));
        para.putDouble(VOL_ALARM, getNormalizationVolume(VOL_ALARM));
        para.putDouble(
          VOL_NOTIFICATION,
          getNormalizationVolume(VOL_NOTIFICATION)
        );
        try {
          mContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("RNVMEventVolume", para);
        } catch (RuntimeException e) {
          // Possible to interact with volume before JS bundle execution is finished.
          // This is here to avoid app crashing.
        }
      }
    }
  }
}
