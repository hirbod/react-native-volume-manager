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
import android.view.ViewTreeObserver;
import android.widget.EditText;
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

/**
 * Manages volume control and hardware button interactions for React Native.
 * Handles volume changes, hardware button events, and volume UI visibility.
 */
@ReactModule(name = VolumeManagerModule.NAME)
public class VolumeManagerModule extends ReactContextBaseJavaModule implements ActivityEventListener, LifecycleEventListener {
  public static final String NAME = "VolumeManager";
  private final String TAG = VolumeManagerModule.class.getSimpleName();

  // Volume stream type constants
  private static final String VOL_VOICE_CALL = "call";
  private static final String VOL_SYSTEM = "system";
  private static final String VOL_RING = "ring";
  private static final String VOL_MUSIC = "music";
  private static final String VOL_ALARM = "alarm";
  private static final String VOL_NOTIFICATION = "notification";

  private final ReactApplicationContext mContext;
  private final AudioManager am;
  private VolumeBroadcastReceiver volumeBR;

  // State tracking
  private Boolean showNativeVolumeUI = true;  // Controls visibility of system volume UI
  private Boolean hardwareButtonListenerRegistered = false;  // Tracks key listener state
  private ViewTreeObserver.OnGlobalFocusChangeListener globalFocusListener;  // Handles TextInput focus
  private Boolean volumeMonitoringEnabled = false; // Tracks if volume monitoring is enabled

  String category;

  public VolumeManagerModule(ReactApplicationContext reactContext) {
    super(reactContext);
    mContext = reactContext;
    reactContext.addLifecycleEventListener(this);
    am = (AudioManager) mContext.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
    this.category = null;
  }

  /**
   * Registers broadcast receiver for volume changes if not already registered
   */
  private void registerVolumeReceiver() {
    if (volumeBR == null) {
      volumeBR = new VolumeBroadcastReceiver();
    }

    if (!volumeBR.isRegistered()) {
      IntentFilter filter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
      mContext.registerReceiver(volumeBR, filter);
      volumeBR.setRegistered(true);
    }
  }

  /**
   * Safely unregisters the volume broadcast receiver
   */
  private void unregisterVolumeReceiver() {
    if (volumeBR != null && volumeBR.isRegistered()) {
      try {
        mContext.unregisterReceiver(volumeBR);
        volumeBR.setRegistered(false);
      } catch (IllegalArgumentException e) {
        Log.e(TAG, "Error unregistering volume receiver", e);
      }
    }
  }

  /**
   * Sets up hardware volume button handling and focus management.
   * Handles volume key events and TextInput focus changes.
   */
  private void setupKeyListener() {
    runOnUiThread(() -> {
      if (hardwareButtonListenerRegistered) return;
      if (mContext.getCurrentActivity() == null) return;

      Activity activity = mContext.getCurrentActivity();
      View contentView = activity.findViewById(android.R.id.content);

      // Handles focus changes between TextInputs and other views
      // Restores volume key functionality when leaving TextInput
      globalFocusListener = new ViewTreeObserver.OnGlobalFocusChangeListener() {
        @Override
        public void onGlobalFocusChanged(View oldFocus, View newFocus) {
          if (oldFocus instanceof EditText && !(newFocus instanceof EditText)) {
            contentView.requestFocus();
          }
        }
      };

      contentView.getViewTreeObserver().addOnGlobalFocusChangeListener(globalFocusListener);
      contentView.setOnKeyListener(null);  // Clear any existing listeners
      contentView.setFocusableInTouchMode(true);
      contentView.requestFocus();

      // Handle volume key events when native UI is hidden
      contentView.setOnKeyListener((v, keyCode, event) -> {
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
      hardwareButtonListenerRegistered = true;
    });
  }

  /**
   * Cleans up key and focus listeners.
   * Called during cleanup and app state changes.
   */
  private void cleanupKeyListener() {
    runOnUiThread(() -> {
      if (!hardwareButtonListenerRegistered) return;
      if (mContext.getCurrentActivity() == null) {
        hardwareButtonListenerRegistered = false;
        return;
      }

      Activity activity = mContext.getCurrentActivity();
      View contentView = activity.findViewById(android.R.id.content);
      if (globalFocusListener != null) {
        contentView.getViewTreeObserver().removeOnGlobalFocusChangeListener(globalFocusListener);
        globalFocusListener = null;
      }
      contentView.setOnKeyListener(null);
      hardwareButtonListenerRegistered = false;
    });
  }

  // React Native methods

  @ReactMethod
  public void showNativeVolumeUI(ReadableMap config) {
    showNativeVolumeUI = config.getBoolean("enabled");
    setupKeyListener();  // Reset key listener state based on new UI preference
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  // Keep: Required no-op methods for iOS compatibility
  @ReactMethod
  public void enable(final Boolean enabled, final Boolean async) {
    // no op
  }

  @ReactMethod
  public void setCategory(final String category, final Boolean mixWithOthers) {
    // no op
  }

  @ReactMethod
  public void addListener(String eventName) {
    if (eventName.equals("RNVMEventVolume")) {
      volumeMonitoringEnabled = true;
      registerVolumeReceiver();
    }
  }

  @ReactMethod
  public void removeListeners(int count) {
    volumeMonitoringEnabled = false;
    unregisterVolumeReceiver();
  }

  // Volume and ringer mode methods

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

  /**
   * Sets volume for specified stream type with optional UI and sound feedback
   */
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
      am.setStreamVolume(volType, (int) (val * am.getStreamMaxVolume(volType)), flags);
    } catch (SecurityException e) {
      if (val == 0) {
        Log.w(TAG, "setVolume(0) failed. See https://github.com/c19354837/react-native-system-setting/issues/48");
        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted()) {
          Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
          mContext.startActivity(intent);
        }
      }
      Log.e(TAG, "err", e);
    }
    registerVolumeReceiver();
  }

  /**
   * Returns current volume levels for all stream types
   */
  @ReactMethod
  public void getVolume(Promise promise) {
    WritableMap result = Arguments.createMap();
    result.putDouble("volume", getNormalizationVolume(VOL_MUSIC));
    result.putDouble(VOL_VOICE_CALL, getNormalizationVolume(VOL_VOICE_CALL));
    result.putDouble(VOL_SYSTEM, getNormalizationVolume(VOL_SYSTEM));
    result.putDouble(VOL_RING, getNormalizationVolume(VOL_RING));
    result.putDouble(VOL_MUSIC, getNormalizationVolume(VOL_MUSIC));
    result.putDouble(VOL_ALARM, getNormalizationVolume(VOL_ALARM));
    result.putDouble(VOL_NOTIFICATION, getNormalizationVolume(VOL_NOTIFICATION));
    promise.resolve(result);
  }

  /**
   * Returns normalized volume (0-1) for the given stream type
   */
  private float getNormalizationVolume(String type) {
    int volType = getVolType(type);
    return am.getStreamVolume(volType) * 1.0f / am.getStreamMaxVolume(volType);
  }

  /**
   * Maps string type to AudioManager stream constant
   */
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

  // Lifecycle methods

  @Override
  public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {}

  @Override
  public void onNewIntent(Intent intent) {}

  /**
   * Restores volume control state when app comes to foreground
   */
  @Override
  public void onHostResume() {
    runOnUiThread(() -> {
      Activity activity = mContext.getCurrentActivity();
      if (activity != null) {
        View contentView = activity.findViewById(android.R.id.content);
        contentView.clearFocus();  // Clear existing focus
        contentView.requestFocus();  // Request fresh focus
      }
    });
    hardwareButtonListenerRegistered = false;  // Force re-setup of listeners
    setupKeyListener();

    if (volumeMonitoringEnabled) {
      registerVolumeReceiver();
    }
  }

  /**
   * Cleans up listeners when app goes to background
   */
  @Override
  public void onHostPause() {
    if (volumeMonitoringEnabled) {
      unregisterVolumeReceiver();
    }
    cleanupKeyListener();
  }

  /**
   * Final cleanup when app is destroyed
   */
  @Override
  public void onHostDestroy() {
    cleanupKeyListener();
  }

  /**
   * BroadcastReceiver that handles volume change events and emits them to JS
   */
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
        int streamType = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);

        WritableMap para = Arguments.createMap();

        // Only emit volume change for the specific stream that changed
        switch (streamType) {
          case AudioManager.STREAM_VOICE_CALL:
            para.putDouble("volume", getNormalizationVolume(VOL_VOICE_CALL));
            para.putString("type", VOL_VOICE_CALL);
            break;
          case AudioManager.STREAM_SYSTEM:
            para.putDouble("volume", getNormalizationVolume(VOL_SYSTEM));
            para.putString("type", VOL_SYSTEM);
            break;
          case AudioManager.STREAM_RING:
            para.putDouble("volume", getNormalizationVolume(VOL_RING));
            para.putString("type", VOL_RING);
            break;
          case AudioManager.STREAM_MUSIC:
            para.putDouble("volume", getNormalizationVolume(VOL_MUSIC));
            para.putString("type", VOL_MUSIC);
            break;
          case AudioManager.STREAM_ALARM:
            para.putDouble("volume", getNormalizationVolume(VOL_ALARM));
            para.putString("type", VOL_ALARM);
            break;
          case AudioManager.STREAM_NOTIFICATION:
            para.putDouble("volume", getNormalizationVolume(VOL_NOTIFICATION));
            para.putString("type", VOL_NOTIFICATION);
            break;
          default:
            return;  // Unknown stream type, don't emit event
        }

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