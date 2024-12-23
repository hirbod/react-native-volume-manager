package com.reactnativevolumemanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = VolumeManagerSilentListenerModule.TAG)
class VolumeManagerSilentListenerModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), LifecycleEventListener {

  companion object {
    const val TAG = "VolumeManagerSilentListener"
  }

  private val audioManager: AudioManager =
    reactContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager

  private var isObserverRegistered = false
  private var isSilentMonitoringEnabled = false

  private val receiver: BroadcastReceiver =
    object : BroadcastReceiver() {
      override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AudioManager.RINGER_MODE_CHANGED_ACTION) {
          val silentStatus = Utils.getSilentStatus(audioManager)
          val data = Arguments.createMap()
          data.putBoolean("status", silentStatus.status)
          data.putString("mode", silentStatus.mode.name)
          Utils.sendEventToReactNative(
            eventName = "RNVMSilentEvent",
            reactContext = reactApplicationContext,
            params = data
          )
        }
      }
    }

  init {
    reactContext.addLifecycleEventListener(this)
  }

  override fun getName(): String {
    return TAG
  }

  @ReactMethod
  fun addListener(eventName: String?) {
    if (eventName == "RNVMSilentEvent") {
      isSilentMonitoringEnabled = true
      safeRegisterObserver()
    }
  }

  @ReactMethod
  fun removeListeners(count: Int) {
    isSilentMonitoringEnabled = false
    safeUnregisterObserver()
  }

  @ReactMethod
  fun isDeviceSilent(promise: Promise) {
    val silentStatus = Utils.getSilentStatus(audioManager)
    promise.resolve(silentStatus.status)
  }

  @ReactMethod
  fun registerObserver() {
    isSilentMonitoringEnabled = true
    safeRegisterObserver()
  }

  @ReactMethod
  fun unregisterObserver() {
    isSilentMonitoringEnabled = false
    safeUnregisterObserver()
  }

  private fun safeRegisterObserver() {
    if (!isObserverRegistered && isSilentMonitoringEnabled) {
      try {
        val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        reactApplicationContext.registerReceiver(receiver, filter)
        isObserverRegistered = true
      } catch (e: Exception) {
      }
    }
  }

  private fun safeUnregisterObserver() {
    if (isObserverRegistered) {
      try {
        reactApplicationContext.unregisterReceiver(receiver)
        isObserverRegistered = false
      } catch (e: Exception) {
      }
    }
  }

  override fun onHostResume() {
    if (isSilentMonitoringEnabled) {
      safeRegisterObserver()
    }
  }

  override fun onHostPause() {
    if (isSilentMonitoringEnabled) {
      safeUnregisterObserver()
    }
  }

  override fun onHostDestroy() {
    safeUnregisterObserver()
  }
}