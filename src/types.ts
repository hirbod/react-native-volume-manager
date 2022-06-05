export type eventCallback = (isMuted: boolean) => void;
export type setCheckIntervalType = (newInterval: number) => void;

export type AndroidVolumeTypes =
  | 'music'
  | 'call'
  | 'system'
  | 'ring'
  | 'alarm'
  | 'notification';

export interface VolumeManagerSetVolumeConfig {
  /**
   * boolean indicating whether to play a sound on volume change
   * @default false
   */
  playSound?: boolean;
  /**
   * @Platform Android only, no-op on iOS.
   * @description Android has different volume modes. Default is 'music', which is the same as 'system'.
   * @description Available types are 'music', 'call', 'system', 'ring', 'alarm', 'notification'.
   * @default 'music'
   */
  type?: AndroidVolumeTypes;
  /**
   * boolean indicating whether to show the native volume UI.
   * @default false
   */
  showUI?: boolean;
}
export interface VolumeResult {
  /** iOS and Android */
  volume: number;
  /** Android only result types */
  alarm?: number;
  call?: number;
  music?: number;
  notification?: number;
  ring?: number;
  system?: number;
}

// Accepted Ringer Mode values
export const RINGER_MODE = {
  silent: 0,
  vibrate: 1,
  normal: 2,
} as const;

// Ringer Mode type definition
type ValueOf<T> = T[keyof T];
export type RingerModeType = ValueOf<typeof RINGER_MODE>;

/**
 * ## MODE
 *
 * - SILENT: When device is in do not disturb mode
 * - VIBRATE: When device is in vibrate mode
 * - NORMAL: When device is in normal noisy mode
 * - MUTED: When device is with volume 0
 */
export enum Mode {
  SILENT = 'SILENT',
  VIBRATE = 'VIBRATE',
  NORMAL = 'NORMAL',
  MUTED = 'MUTED',
}

export type RingerSilentStatus = {
  status: boolean;
  mode: Mode;
};

export type RingerEventCallback = (event: RingerSilentStatus) => void;

export interface EmitterSubscriptionNoop {
  remove(): void;
}
