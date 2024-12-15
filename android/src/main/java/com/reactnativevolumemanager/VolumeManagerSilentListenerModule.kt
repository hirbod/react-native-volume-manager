package com.reactnativevolumemanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = VolumeManagerSilentListenerModule.TAG)
class VolumeManagerSilentListenerModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val TAG = "VolumeManagerSilentListener"
    }

    private val audioManager: AudioManager =
        reactApplicationContext.getSystemService(AUDIO_SERVICE) as AudioManager

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

    override fun getName(): String {
        return TAG
    }

    @ReactMethod
    @Suppress("UNUSED_PARAMETER")
    fun addListener(eventName: String?) {
        // Keep: Required for RN built in Event Emitter Calls.
    }

    @ReactMethod
    @Suppress("UNUSED_PARAMETER")
    fun removeListeners(count: Int) {
        // Keep: Required for RN built in Event Emitter Calls.
    }

    @ReactMethod
    fun isEnabled(promise: Promise) {
        val silentStatus = Utils.getSilentStatus(audioManager)
        promise.resolve(silentStatus.status)
    }

    @ReactMethod
    fun registerObserver() {
        val filter = IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION)
        reactApplicationContext.registerReceiver(receiver, filter)
    }

    @ReactMethod
    fun unregisterObserver() {
        reactApplicationContext.unregisterReceiver(receiver)
    }
}