import { useState, useEffect } from 'react';
import { getRingerMode, setRingerMode } from './native';
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
