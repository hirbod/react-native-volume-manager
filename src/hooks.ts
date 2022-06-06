import { useState, useEffect } from 'react';
import {
  addSilentListener,
  getRingerMode,
  setNativeSilenceCheckInterval,
  setRingerMode,
} from './native';
import type { RingerModeType } from './types';

/**
 * A hook to get the current ringer mode. You can also change the mode.
 * @example
 * ```ts
  const { mode, setMode, error } = useRingerMode();
 * ```
 */
export const useRingerMode = () => {
  const [mode, setCurrentMode] = useState<RingerModeType | undefined>();
  const [error, setError] = useState<any>(null);

  useEffect(() => {
    (async () => {
      try {
        const currentMode = await getRingerMode();
        setCurrentMode(currentMode);
      } catch (err: any) {
        setError(err);
      }
    })();
  }, []);

  const setMode = async (newMode: RingerModeType) => {
    setError(null);

    try {
      const currentMode = await setRingerMode(newMode);
      setCurrentMode(currentMode);
    } catch (err: any) {
      setError(err);
    }
  };

  return { mode, error, setMode };
};

/**
 * A hook to check if the iOS device is silent. (Silent switch status)
 * @platform iOS
 * @param {number} nativeIntervalCheck The native interval to check the status in seconds. 0.5, default 2.
 * @returns boolean on iOS, undefined on other platforms
 * @example
 * ```ts
  const isSilent = useSilentSwitch(nativeIntervalCheck?: number);
 * ```
 */
export const useSilentSwitch = (nativeIntervalCheck?: number) => {
  const [isSilent, setIsSilent] = useState<boolean | undefined>();

  if (nativeIntervalCheck) setNativeSilenceCheckInterval(nativeIntervalCheck);

  useEffect(() => {
    const silentListener = addSilentListener((status) => {
      setIsSilent(status);
    });

    return function unmountSilentSwitchListener() {
      silentListener.remove();
    };
  }, []);

  return isSilent;
};
