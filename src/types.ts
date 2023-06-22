/**
 * Represents the mute switch status of the ring.
 * @export
 * @interface RingMuteSwitchStatus
 * @property {boolean} isMuted - Indicates if the ring is muted.
 * @property {boolean} initialQuery - Represents the initial query status.
 */
export type RingMuteSwitchStatus = {
  isMuted: boolean;
  initialQuery: boolean;
};

/**
 * Called when there is a ring mute switch event.
 * @export
 * @callback
 * @param {RingMuteSwitchStatus} status - The current mute switch status.
 */
export type RingMuteSwitchEventCallback = (
  status: RingMuteSwitchStatus
) => void;

/**
 * Used to set the interval check.
 * @export
 * @callback
 * @param {number} newInterval - The new interval to be set.
 */
export type setCheckIntervalType = (newInterval: number) => void;

/**
 * Categories of AV Audio sessions.
 * @export
 */
export type AVAudioSessionCategory =
  | 'Ambient'
  | 'SoloAmbient'
  | 'Playback'
  | 'Record'
  | 'PlayAndRecord'
  | 'AudioProcessing'
  | 'MultiRoute'
  | 'Alarm';

/**
 * Modes of AV Audio sessions.
 * @export
 */
export type AVAudioSessionMode =
  | 'Default'
  | 'VoiceChat'
  | 'VideoChat'
  | 'GameChat'
  | 'VideoRecording'
  | 'Measurement'
  | 'MoviePlayback'
  | 'SpokenAudio';

/**
 * Types of volume on Android.
 * @export
 */
export type AndroidVolumeTypes =
  | 'music'
  | 'call'
  | 'system'
  | 'ring'
  | 'alarm'
  | 'notification';

/**
 * The configuration settings for setting the volume.
 * @export
 * @interface VolumeManagerSetVolumeConfig
 * @property {boolean} playSound - Indicates whether to play a sound on volume change. Default is false.
 * @property {AndroidVolumeTypes} type - Defines the type of volume to change. Only applicable to Android. Default is 'music'.
 * @property {boolean} showUI - Indicates whether to show the native volume UI. Default is false.
 */
export interface VolumeManagerSetVolumeConfig {
  playSound?: boolean;
  type?: AndroidVolumeTypes;
  showUI?: boolean;
}

/**
 * Represents the volume result.
 * @export
 * @interface VolumeResult
 * @property {number} volume - The volume level. Both for iOS and Android. Defaults to music.
 * @property {number} alarm - The alarm volume. Android only.
 * @property {number} call - The call volume. Android only.
 * @property {number} music - The music volume. Android only.
 * @property {number} notification - The notification volume. Android only.
 * @property {number} ring - The ring volume. Android only.
 * @property {number} system - The system volume. Android only.
 */
export interface VolumeResult {
  // Both iOS and Android (defaults to type music for android)
  volume: number;
  // Android only
  alarm?: number;
  // Android only
  call?: number;
  // Android only
  music?: number;
  // Android only
  notification?: number;
  // Android only
  ring?: number;
  // Android only
  system?: number;
}

// Accepted Ringer Mode values
export const RINGER_MODE = {
  silent: 0,
  vibrate: 1,
  normal: 2,
} as const;

/**
 * Represents the ringer mode.
 * @export
 * @typedef {0 | 1 | 2} RingerModeType
 */
export type RingerModeType = (typeof RINGER_MODE)[keyof typeof RINGER_MODE];

/**
 * Modes for the device.
 * @export
 * @enum {string}
 */
export enum Mode {
  SILENT = 'SILENT',
  VIBRATE = 'VIBRATE',
  NORMAL = 'NORMAL',
  MUTED = 'MUTED',
}

/**
 * Represents the silent status of the ringer.
 * @export
 * @interface RingerSilentStatus
 * @property {boolean} status - Indicates if the ringer is silent.
 * @property {Mode} mode - The current mode of the device.
 */
export type RingerSilentStatus = {
  status: boolean;
  mode: Mode;
};

/**
 * Called when there is a ringer event.
 * @export
 * @callback
 * @param {RingerSilentStatus} event - The ringer event.
 */
export type RingerEventCallback = (event: RingerSilentStatus) => void;

/**
 * Represents a subscription to an event that has a method to remove it.
 * @export
 * @interface EmitterSubscriptionNoop
 */
export interface EmitterSubscriptionNoop {
  remove(): void;
}
