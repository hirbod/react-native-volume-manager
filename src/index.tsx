import {
  NativeModules,
  NativeEventEmitter,
  Platform,
  EmitterSubscription,
} from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-volume-api' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo managed workflow\n';

export type AndroidVolumeTypes =
  | 'music'
  | 'call'
  | 'system'
  | 'ring'
  | 'alarm'
  | 'notification';

export interface VolumeManagerSetVolumeConfig {
  playSound?: boolean;
  type?: AndroidVolumeTypes;
  showUI?: boolean;
}
export interface VolumeResult {
  volume: number; // returns a value between 0 and 1 (float)
}

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

const eventEmitter = new NativeEventEmitter(VolumeManagerNativeModule);

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
): Promise<VolumeResult> {
  return await VolumeManagerNativeModule.getVolume(type);
}

/**
 * Set the current device volume.
 * @param {number} value - The volume value to set. Must be between 0 and 1 (float).
 * @param {VolumeManagerSetVolumeConfig} config - An object with the following properties:
 *
 * @example
 * ```ts
 * // set the volume to 0.5 (50%) and play a sound when the volume is changed
 * VolumeManager.setVolume(0.5, { playSound: true, type: 'music', showUI: true });
 * ```
 */
export function setVolume(
  value: number,
  config: VolumeManagerSetVolumeConfig = {}
): void {
  config = Object.assign(
    {
      playSound: false,
      type: 'music',
      showUI: false,
    },
    config
  );
  VolumeManagerNativeModule.setVolume(value, config);
}

export function addListener(
  callback: (...arg: any[]) => VolumeResult
): EmitterSubscription {
  return eventEmitter.addListener('EventVolume', callback);
}

export const VolumeManager = { addListener, getVolume, setVolume };

export default VolumeManager;
