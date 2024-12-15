import type {
  AVAudioSessionCategory,
  AVAudioSessionMode,
  EmitterSubscriptionNoop,
  RingMuteSwitchEventCallback,
  RingerEventCallback,
  RingerModeType,
  VolumeManagerSetVolumeConfig,
  VolumeResult,
} from './types';

// Track if warning has been shown
let hasWarned = false;

// Helper function to show warning only in development
const warnOnWeb = () => {
  if (__DEV__ && !hasWarned) {
    console.warn(
      'react-native-volume-manager is not functional on the web. While the package exports no-op methods for web usage, allowing you to include it without any issues, these methods have no actual effect. This warning is only visible in development mode.'
    );
    hasWarned = true;
  }
};

const noopEmitterSubscription: EmitterSubscriptionNoop = {
  remove: () => {
    // noop
  },
};

// Base volume result for web platform
const defaultVolumeResult: VolumeResult = {
  volume: 1,
};

export async function getRingerMode(): Promise<RingerModeType | undefined> {
  warnOnWeb();
  return undefined;
}

export async function setRingerMode(
  _mode: RingerModeType
): Promise<RingerModeType | undefined> {
  warnOnWeb();
  return undefined;
}

export async function enable(
  _enabled: boolean = true,
  _async: boolean = true
): Promise<void> {
  warnOnWeb();
  return undefined;
}

export async function setActive(
  _value: boolean = true,
  _async: boolean = true
): Promise<void> {
  warnOnWeb();
  return undefined;
}

export async function setCategory(
  _value: AVAudioSessionCategory,
  _mixWithOthers: boolean = false
): Promise<void> {
  warnOnWeb();
  return undefined;
}

export async function setMode(_value: AVAudioSessionMode): Promise<void> {
  warnOnWeb();
  return undefined;
}

export async function enableInSilenceMode(
  _enabled: boolean = true
): Promise<void> {
  warnOnWeb();
  return undefined;
}

export async function checkDndAccess(): Promise<boolean | undefined> {
  warnOnWeb();
  return undefined;
}

export async function requestDndAccess(): Promise<boolean | undefined> {
  warnOnWeb();
  return undefined;
}

export async function getVolume(): Promise<VolumeResult> {
  warnOnWeb();
  return defaultVolumeResult;
}

export async function setVolume(
  _value: number,
  _config: VolumeManagerSetVolumeConfig = {}
): Promise<void> {
  warnOnWeb();
  return undefined;
}

export async function showNativeVolumeUI(_config: {
  enabled: boolean;
}): Promise<void> {
  warnOnWeb();
  return undefined;
}

export function addVolumeListener(
  _callback: (result: VolumeResult) => void
): EmitterSubscriptionNoop {
  warnOnWeb();
  return noopEmitterSubscription;
}

export const addSilentListener = (
  _callback: RingMuteSwitchEventCallback
): EmitterSubscriptionNoop => {
  warnOnWeb();
  return noopEmitterSubscription;
};

export const setNativeSilenceCheckInterval = (_value: number): void => {
  warnOnWeb();
  // noop
};

export const isAndroidDeviceSilent = (): Promise<boolean | null> => {
  warnOnWeb();
  return Promise.resolve(null);
};

export const addRingerListener = (
  _callback: RingerEventCallback
): EmitterSubscriptionNoop => {
  warnOnWeb();
  return noopEmitterSubscription;
};

export const removeRingerListener = (
  _listener: EmitterSubscriptionNoop
): void => {
  warnOnWeb();
  // noop
};

export const VolumeManager = {
  addVolumeListener,
  getVolume,
  setVolume,
  showNativeVolumeUI,
  isAndroidDeviceSilent,
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
