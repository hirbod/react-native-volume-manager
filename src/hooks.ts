import { useState, useEffect } from 'react';
import {
  addSilentListener,
  getRingerMode,
  setNativeSilenceCheckInterval,
  setRingerMode,
} from './native';
import type { RingerModeType } from './types';

/**
 * `useRingerMode` is a custom hook to get the current ringer mode of the device.
 * It also provides a function to change the ringer mode. This hook is intended to be used in react components where ringer mode needs to be managed.
 *
 * @returns {Object} - Contains the current mode, any error occurred, and a function to set a new mode.
 * @property {RingerModeType | undefined} mode - The current mode of the device's ringer.
 * @property {any} error - Any error that occurred while getting or setting the ringer mode.
 * @property {Function} setMode - Function to set a new ringer mode.
 *
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
 * `useSilentSwitch` is a custom hook to check if the iOS device is in silent mode.
 *
 * @param {number} nativeIntervalCheck - The native interval to check the status in seconds. Default is 2 seconds.
 *
 * @returns {Object | undefined} - Contains boolean properties `isMuted` and `initialQuery` on iOS. Returns `undefined` for the first call and on non-iOS platforms.
 * @property {boolean} isMuted - Represents the ring/mute switch position.
 * @property {boolean} initialQuery - Informs whether reported status is the very first one reported.
 *
 * @platform iOS
 *
 * @example
 * ```ts
  const { isMuted, initialQuery } = useSilentSwitch(nativeIntervalCheck?: number);
 * ```
 */
export const useSilentSwitch = (nativeIntervalCheck?: number) => {
  const [status, setStatus] = useState<
    { isMuted: boolean; initialQuery: boolean } | undefined
  >();

  if (nativeIntervalCheck) setNativeSilenceCheckInterval(nativeIntervalCheck);

  useEffect(() => {
    const silentListener = addSilentListener((event) => {
      setStatus(event);
    });

    return function unmountSilentSwitchListener() {
      silentListener.remove();
    };
  }, []);

  return status;
};
