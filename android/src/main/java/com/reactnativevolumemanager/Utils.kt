package com.reactnativevolumemanager

import android.media.AudioManager
import com.facebook.react.bridge.ReactContext
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule
import kotlin.math.ln

enum class MODE {
  SILENT,
  VIBRATE,
  NORMAL,
  MUTED,
}

class Utils {
  companion object {
    fun sendEventToReactNative(reactContext: ReactContext,
                               eventName: String,
                               params: WritableMap) {
      reactContext
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
        .emit(eventName, params)
    }

    fun getSilentStatus(audioManager: AudioManager): VolumeManagerSilentListenerStatus {
      when (audioManager.ringerMode) {
        AudioManager.RINGER_MODE_SILENT -> {
          return VolumeManagerSilentListenerStatus(status = true, mode = MODE.SILENT)
        }
        AudioManager.RINGER_MODE_VIBRATE -> {
          return VolumeManagerSilentListenerStatus(status = true, mode = MODE.VIBRATE)
        }
        AudioManager.RINGER_MODE_NORMAL -> {
          val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
          val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

          val volume = (1 - (ln((maxVolume - currentVolume).toDouble()) /
            ln(maxVolume.toDouble())))

          return if (volume > 0) {
            VolumeManagerSilentListenerStatus(status = false, mode = MODE.NORMAL)
          } else {
            VolumeManagerSilentListenerStatus(status = true, mode = MODE.MUTED)
          }
        }
        else -> return VolumeManagerSilentListenerStatus(status = false, mode = MODE.NORMAL)
      }
    }
  }
}
