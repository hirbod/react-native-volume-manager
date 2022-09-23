import {
  NativeModules,
  NativeEventEmitter,
  Platform,
  EmitterSubscription,
} from 'react-native';
import type {
  AndroidVolumeTypes,
  AVAudioSessionCategory,
  AVAudioSessionMode,
  EmitterSubscriptionNoop,
  eventCallback,
  RingerEventCallback,
  RingerModeType,
  setCheckIntervalType,
  VolumeManagerSetVolumeConfig,
  VolumeResult,
} from './types';

const LINKING_ERROR =
  `The package 'react-native-volume-manager' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

const VolumeManagerNativeModule = NativeModules.VolumeManager
  ? NativeModules.VolumeManager
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const SilentListenerNativeModule = NativeModules.VolumeManagerSilentListener
  ? NativeModules.VolumeManagerSilentListener
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const noopEmitterSubscription = {
  remove: () => {
    // noop
  },
} as EmitterSubscriptionNoop;

const eventEmitter = new NativeEventEmitter(VolumeManagerNativeModule);
const silentEventEmitter = new NativeEventEmitter(SilentListenerNativeModule);
const isAndroid = Platform.OS === 'android';

export async function getRingerMode(): Promise<RingerModeType | undefined> {
  if (!isAndroid) {
    return;
  }

  return VolumeManagerNativeModule.getRingerMode();
}

export async function setRingerMode(
  mode: RingerModeType
): Promise<RingerModeType | undefined> {
  if (!isAndroid) {
    return;
  }

  return VolumeManagerNativeModule.setRingerMode(mode);
}

export async function enable(
  enabled: boolean = true,
  async: boolean = true
): Promise<void> {
  return VolumeManagerNativeModule.enable(enabled, async);
}

export async function setActive(
  value: boolean = true,
  async: boolean = true
): Promise<void> {
  if (!isAndroid) {
    return VolumeManagerNativeModule.setActive(value, async);
  }
  return undefined;
}

export async function setCategory(
  value: AVAudioSessionCategory,
  mixWithOthers?: boolean
): Promise<void> {
  if (!isAndroid) {
    return VolumeManagerNativeModule.setCategory(value, mixWithOthers);
  }
  return undefined;
}

export async function setMode(value: AVAudioSessionMode): Promise<void> {
  if (!isAndroid) {
    return VolumeManagerNativeModule.setMode(value);
  }
  return undefined;
}

export async function enableInSilenceMode(
  enabled: boolean = true
): Promise<void> {
  if (isAndroid) {
    return undefined;
  }

  return VolumeManagerNativeModule.enableInSilenceMode(enabled);
}

export async function checkDndAccess(): Promise<boolean | undefined> {
  if (!isAndroid) {
    return;
  }

  return VolumeManagerNativeModule.checkDndAccess();
}

export async function requestDndAccess(): Promise<boolean | undefined> {
  if (!isAndroid) {
    return;
  }

  return VolumeManagerNativeModule.requestDndAccess();
}

/**
 * Get the current device volume.
 * @param {AndroidVolumeTypes} type - The type of volume you want to retrieve. Defaults to 'music' (Android only, no-op on iOS).
 * @returns {Promise<VolumeResult>} - Returns a promise that resolves to an object with the volume value.
 *
 * @example
 * ```ts
 * const { volume } = await VolumeManager.getVolume('music'); // type is no-op on iOS
 * ```
 */
export async function getVolume(
  type: AndroidVolumeTypes = 'music'
): Promise<VolumeResult | number> {
  return await VolumeManagerNativeModule.getVolume(type);
}

/**
 * Set the current device volume.
 * @param {number} value - The volume value to set. Must be between 0 and 1 (float).
 * @param {VolumeManagerSetVolumeConfig} config - An object with the following properties:
 * - playSound: boolean indicating whether to play a sound on volume change.
 * - type: Default is 'music', which is the same as 'system'. Available types are 'music', 'call', 'system', 'ring', 'alarm', 'notification'.
 * - showUI: boolean indicating whether to show the native volume UI.
 * @example
 * ```ts
 * // set the volume to 0.5 (50%) and play a sound when the volume is changed
 * VolumeManager.setVolume(0.5, { playSound: true, type: 'music', showUI: true });
 * ```
 */
export async function setVolume(
  value: number,
  config: VolumeManagerSetVolumeConfig = {}
): Promise<void> {
  config = Object.assign(
    {
      playSound: false,
      type: 'music',
      showUI: false,
    },
    config
  );
  return await VolumeManagerNativeModule.setVolume(value, config);
}

export function addVolumeListener(
  callback: (result: VolumeResult) => void
): EmitterSubscription {
  return eventEmitter.addListener('RNVMEventVolume', callback);
}

// SilentListener related
export const addSilentListener = (
  callback: eventCallback
): EmitterSubscription | EmitterSubscriptionNoop => {
  if (Platform.OS === 'ios') {
    return silentEventEmitter.addListener('RNVMSilentEvent', callback);
  }

  return noopEmitterSubscription;
};

export const setNativeSilenceCheckInterval: setCheckIntervalType = (
  value: number
) => {
  if (Platform.OS === 'ios') {
    SilentListenerNativeModule.setInterval(value);
  }
};

// Ringer mode listener

export const isRingerListenerEnabled = (): Promise<boolean> => {
  if (Platform.OS === 'android') {
    return SilentListenerNativeModule.isEnabled();
  }
  return Promise.resolve(true);
};

export const addRingerListener = (
  callback: RingerEventCallback
): EmitterSubscription | EmitterSubscriptionNoop => {
  if (Platform.OS === 'android') {
    SilentListenerNativeModule.registerObserver();
    return silentEventEmitter.addListener('RNVMSilentEvent', callback);
  }
  return noopEmitterSubscription;
};

export const removeRingerListener = (
  listener: EmitterSubscription | EmitterSubscriptionNoop
): void => {
  if (Platform.OS === 'android') {
    SilentListenerNativeModule.unregisterObserver();
    listener && listener.remove();
  }
};

export const VolumeManager = {
  addVolumeListener,
  getVolume,
  setVolume,
  isRingerListenerEnabled,
  addSilentListener,
  addRingerListener,
  removeRingerListener,
  setNativeSilenceCheckInterval,
  getRingerMode,
  setRingerMode,
  checkDndAccess,
  requestDndAccess,
  enable,
  setActive,
  setCategory,
  setMode,
  enableInSilenceMode,
};

export default VolumeManager;
