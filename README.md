![React Native Volume Manager Ringer Mute Silent Switch](gh-banner.png)

# react-native-volume-manager

This native package enhances system volume control on both iOS and Android, allowing you to adjust the volume, monitor volume changes, and suppress the default volume UI to create your own custom volume slider and user experience. The package offers a straightforward API for obtaining the current volume level. On iOS, you can determine if the silent switch is enabled and track any changes, while on Android, you can modify and monitor ringer mode changes.

| ![React Native Volume Manager](ios-preview.gif) | ![React Native Volume Manager](android-preview.gif) |
| ----------------------------------------------- | --------------------------------------------------- |

## Notice

This library is **incompatible** with simulators and emulators and functions exclusively on **real devices**. Android Emulator 33 and above is supported.

## Installation

```sh
# Using npm
npm install react-native-volume-manager

# using yarn
yarn add react-native-volume-manager
```

#### Using React Native >= 0.60

Linking the package manually is not required anymore with [Autolinking](https://github.com/react-native-community/cli/blob/master/docs/autolinking.md).

## Expo

This library incorporates native code and is incompatible with _Expo Go_. However, you can effortlessly install it using a [custom dev client](https://docs.expo.dev/development/getting-started/) as recommended in 2022.

**No config plugin required.**

---

## Usage 🚀

All methods are available under the `VolumeManager` namespace or can be imported directly.
(eg. `addVolumeListener`)

```tsx
import { VolumeManager } from 'react-native-volume-manager';

// Enable or disable the native volume toast globally (iOS, Android)
VolumeManager.showNativeVolumeUI({ enabled: true }); // default is true

// Set the volume (value between 0 and 1)
await VolumeManager.setVolume(0.5); // float value between 0 and 1

// Set volume with additional options
await VolumeManager.setVolume(0.5, {
  type: 'system', // default: "music" (Android only)
  showUI: true, // default: false (suppress native UI volume toast for iOS & Android)
  playSound: false, // default: false (Android only)
});

// Get the current volume async (type defaults to "music")
const { volume } = await VolumeManager.getVolume({ type: 'music' });

// Listen to volume changes (example)
useEffect(() => {
  const volumeListener = VolumeManager.addVolumeListener((result) => {
    console.log(result.volume); // current volume as a float (0-1)

    // On Android, the result object also has the keys:
    // music, system, ring, alarm, notification
  });

  // Clean up function
  return () => {
    volumeListener.remove(); // remove listener
  };
}, []);

// With useFocusEffect from react-navigation
useFocusEffect(
  React.useCallback(() => {
    const volumeListener = VolumeManager.addVolumeListener(async (result) => {
      if (Platform.OS === 'ios') {
        try {
          VolumeManager.enableInSilenceMode(true); // Enable audio in silent mode
        } catch {}
      }
    });
    return () => {
      volumeListener.remove(); // remove listener
    };
  }, [])
);
```

## iOS Audio Session Management

AVAudioSession related functions.

```tsx
import { VolumeManager } from 'react-native-volume-manager';

// Enable or disable iOS AudioSession (activates Ambient mode)
// Parameters: enable (boolean), async (default: true)
VolumeManager.enable(true, true); // Enable async
VolumeManager.enable(false, true); // Disable async

// Activate or deactivate the audio session and inform background music to resume
// Parameters: setActive (boolean), async (default: true)
VolumeManager.setActive(true, true); // Activate async
VolumeManager.setActive(false, true); // Deactivate async, non-blocking

// Example: set audio session active based on app state
const onAppStateChange = useCallback(async (status) => {
  if (status === 'active') {
    if (audioSessionIsInactive.current) {
      VolumeManager.setActive(true);
      audioSessionIsInactive.current = false;
    }
  } else if (status === 'background') {
    VolumeManager.setActive(false);
    audioSessionIsInactive.current = true;
  }
}, []);

// AppState listener hook
useAppState({ onChange: onAppStateChange });
```

If you want to change the Audio Category or Audio Mode on iOS, or play music when the silent switch is active, you can call the functions `enableInSilenceMode`, `setCategory`, or `setMode`. Please refer to Apple's documentation to find out what they do.

### Import the VolumeManager from the 'react-native-volume-manager' package:

```ts
import { VolumeManager } from 'react-native-volume-manager';
```

Change the mode using `setMode` with available options:

- Default
- VoiceChat
- VideoChat
- GameChat
- VideoRecording
- Measurement
- MoviePlayback
- SpokenAudio

```javascript
VolumeManager.setMode(mode);
```

Change the category using `setCategory`:

- Ambient
- SoloAmbient
- Playback
- Record
- PlayAndRecord
- MultiRoute
- Alarm

```javascript
VolumeManager.setCategory(value, mixWithOthers); // mixWithOthers defaults to false
```

Enable or disable playing audio when the silent switch is active using `enableInSilenceMode`:

```javascript
VolumeManager.enableInSilenceMode(true); // Enable
VolumeManager.enableInSilenceMode(false); // Disable
```

## iOS mute switch listener

There is no native iOS API to determine the mute switch status on a device.

The common approach to check if the device is muted involves playing a brief silent sound and measuring the playback duration. The check is performed every second by default.

**Note: The check runs on the native main thread, not the JS thread.**

You can customize the check interval by modifying the `VolumeManager.setNativeSilenceCheckInterval(1)` property. The minimum value is `0.5`, and the default is `2`. Generally, the default value is sufficient.

```tsx
import { VolumeManager } from 'react-native-volume-manager';
const [isSilent, setIsSilent] = useState<boolean>();
const [initialQuery, setInitialQuery] = useState<boolean>();

// Optional, default interval is 2 seconds. Set a higher value if
// fast recognition is not critical (saves battery)
VolumeManager.setNativeSilenceCheckInterval(1); // min 0.5, default 2

// ....
// ....

useEffect(() => {
  const silentListener = VolumeManager.addSilentListener((status) => {
    setIsSilent(status.isMuted);
    setInitialQuery(status.initialQuery);
  });

  return () => {
    // Remove listener by calling .remove on the returned emitter
    // Always remember to clean up
    silentListener.remove();
  };
}, []);
```

You can also use the `useSilentSwitch` hook from `react-native-volume-manager` to get the mute switch status.

```tsx
import { useSilentSwitch } from 'react-native-volume-manager';

// ...

const { isMuted, initialQuery } = useSilentSwitch(1); // set interval to 1 second for faster recognition

// `isMuted` returns a boolean value on iOS and `undefined` on any other platform.
// `initialQuery` is a boolean value indicating if the initial query has been performed.
```

Note that you can pass an optional interval parameter to control the frequency at which the native thread looks for changes. The default interval is 2 seconds, but higher values are recommended to save battery. The minimum interval is 0.5 seconds. It is recommended to use a higher interval value when fast recognition is not critical.

## API

| Method                                                                                      | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| ------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Volume**                                                                                  |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `VolumeManager.showNativeVolumeUI(config: { enabled:boolean }) => void`                     | Disable the native volume toast when using hardware buttons (iOS, Android).                                                                                                                                                                                                                                                                                                                                                                                                                                                                    |
| `VolumeManager.getVolume(type?:string) => Promise` (async)                                  | Get the volume. <br><br>`type` must be one of `music`, `call`, `system`, `ring`, `alarm`, `notification`, default is `music`. (Android only, iOS will always report the system volume)                                                                                                                                                                                                                                                                                                                                                         |
| `VolumeManager.setVolume(value:float, config?:object)` (async)                              | Set the system volume by specified value, from 0 to 1. 0 for mute, and 1 for the max volume.<br><br> `config?` can be like `{type: 'music', playSound:true, showUI:true}`<br><br> `type` : must be one of `music`, `call`, `system`, `ring`, `alarm`, `notification`, default is `music`. (Android only) <br>`playSound`: Whether to play a sound when changing the volume, default is `false` (Android only)<br>`showUI`: Show the native system volume UI, default is `false` (**Android & iOS**)                                            |
| `VolumeManager.addVolumeListener(callback)`                                                 | Listen to volume changes (soft- and hardware). `addVolumeListener` will return the listener which is needed for cleanup. Will return a number or object.<br><br>Remove the listener when you don't need it anymore. Store the return of<br>`const listener = VolumeManager.addVolumeListener()` in a variable and call it with `listener.remove()`                                                                                                                                                                                             |
| **iOS AVAudioSession related**                                                              |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `VolumeManager.enableInSilenceMode(value: boolean)`                                         | Enable/Disable audio playback when iOS silent switch is active. Has to be called only once or when you change the Audio category.                                                                                                                                                                                                                                                                                                                                                                                                              |
| `VolumeManager.setCategory(value: AVAudioSessionCategory, mixWithOthers?: boolean)`         | Change category. Allowed values are _Ambient, SoloAmbient, Playback, Record, PlayAndRecord, MultiRoute, Alarm_. The second param is false by default. Please refer to Apples documentation                                                                                                                                                                                                                                                                                                                                                     |
| `VolumeManager.setMode(mode: AVAudioSessionMode)`                                           | Change Audio mode, available options are _Default, VoiceChat, VideoChat, GameChat, VideoRecording, Measurement, MoviePlayback, SpokenAudio_                                                                                                                                                                                                                                                                                                                                                                                                    |
| **Android ringer listener**                                                                 |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `VolumeManager.addRingerListener(callback): RingerSilentStatus => void`                     | **Android only:** Listen to ringer mode changes. Returns an object with type. `RingerSilentStatus`. No-op on iOS                                                                                                                                                                                                                                                                                                                                                                                                                               |
| `VolumeManager.removeRingerListener(listener) => void`                                      | **Android only:** Unlike `addVolumeListener`, you need to call a separate method and pass the return of `addRingerListener`. No-op on iOS                                                                                                                                                                                                                                                                                                                                                                                                      |
| `VolumeManager.isRingerListenerEnabled()`                                                   | Returns bool if listening to ringer mode changes is possible.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| `VolumeManager.getRingerMode() => Promise` (async)                                          | Get the current ringer mode. Returns `RingerModeType`                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `VolumeManager.setRingerMode(mode: RingerModeType) => Promise` (async)                      | Set the ringer mode. Please have a look at the Hooks / Ringer mode section, because there are special cases with DND.                                                                                                                                                                                                                                                                                                                                                                                                                          |
| `requestDndAccess()`                                                                        | Request permission to change ringer mode while in DND or to DND                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `VolumeManager.checkDndAccess()`                                                            | Checks if you have permission to change ringer mode while device is in DND or if you can put the device into DND mode.                                                                                                                                                                                                                                                                                                                                                                                                                         |
| **Hook**<br /><br />`const { mode, error, setMode } = useRingerMode();`                     | Returns state and functions to get or set the current ringer mode. This is a one-time getter, if you need monitoring, use the `addRinterListener` instead.                                                                                                                                                                                                                                                                                                                                                                                     |
| **iOS silent listener**                                                                     |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                |
| `VolumeManager.addSilentListener(callback): RingMuteSwitchStatus => void`                   | Listen to silent switch changes on iOS. Returns on object with type `RingMuteSwitchStatus` consisting of properties `isMuted` (indicates physical switch state) and `initialQuery` (indicates whether reported status is the very first one reported what could be treated as the initial state of the `ring/mute switch` upon application launch). Remove the listener when you don't need it anymore.<br /><br />Store the return of `const listener = VolumeManager.addSilentListener()` in a variable and call it with `listener.remove()` |
| `VolumeManager.setNativeSilenceCheckInterval(number)`                                       | How often the native thread should check of the silent switch state on iOS has changed. Defaults to 2, minimum value is 0.5. Increase the number if you don't need frequent checks (will help against battery drainage)                                                                                                                                                                                                                                                                                                                        |
| **Hook**<br /><br />`const { isMuted, initialQuery } = useSilentSwitch(interval?: number);` | Returns a boolean if the iOS silent switch is active. Returns undefined on other platforms. The interval is optional and controls how often the native thread should check for changes. Defaults to 2 (seconds). Minimum is 0.5.                                                                                                                                                                                                                                                                                                               |

## Hooks / Ringer mode (Android only, no-op on iOS)

How to get and set ringer mode with useRingerMode hook

```tsx
import React from 'react';

import { View, Text, Button } from 'react-native';
import { useRingerMode, RINGER_MODE } from 'react-native-volume-manager';

const modeText = {
  [RINGER_MODE.silent]: 'Silent',
  [RINGER_MODE.normal]: 'Normal',
  [RINGER_MODE.vibrate]: 'Vibrate',
};

export default function App() {
  const { mode, error, setMode } = useRingerMode();

  return (
    <View>
      <Text>Ringer Mode: {mode !== undefined ? modeText[mode] : null}</Text>

      <View>
        <Button title="Silent" onPress={() => setMode(RINGER_MODE.silent)} />
        <Button title="Normal" onPress={() => setMode(RINGER_MODE.normal)} />
        <Button title="Vibrate" onPress={() => setMode(RINGER_MODE.vibrate)} />
      </View>

      <View>
        <Text>{error?.message}</Text>
      </View>
    </View>
  );
}
```

## How to get ringer mode with `getRingerMode`

`getRingerMode` is an async function and resolves the current ringer mode of the device. (Resolves undefined on non-Android devices.)

```tsx
import React, { useEffect, useState } from 'react';

import { View, Text } from 'react-native';
import {
  RINGER_MODE,
  getRingerMode,
  RingerModeType,
} from 'react-native-volume-manager';

const modeText = {
  [RINGER_MODE.silent]: 'Silent',
  [RINGER_MODE.normal]: 'Normal',
  [RINGER_MODE.vibrate]: 'Vibrate',
};

export default function App() {
  const [mode, setMode] = useState<RingerModeType | undefined>();

  useEffect(() => {
    (async () => {
      try {
        const currentMode = await getRingerMode();
        setMode(currentMode);
      } catch (error) {
        console.error(error);
      }
    })();
  }, []);

  return (
    <View>
      <Text>Ringer Mode: {mode !== undefined ? modeText[mode] : null}</Text>
    </View>
  );
}
```

## How to set ringer mode with setRingerMode

`setRingerMode` is an asynchronous function that sets the specified ringer mode on the device and resolves the mode once it is set. It returns undefined on non-Android devices.

```tsx
import React from 'react';

import { View, Button } from 'react-native';
import {
  setRingerMode,
  RINGER_MODE,
  RingerModeType,
} from 'react-native-volume-manager';

export default function App() {
  const setMode = (mode: RingerModeType) => {
    try {
      setRingerMode(mode);
    } catch (error) {
      console.error(error);
    }
  };
  return (
    <View>
      <Button title="Silent" onPress={() => setMode(RINGER_MODE.silent)} />
      <Button title="Normal" onPress={() => setMode(RINGER_MODE.normal)} />
      <Button title="Vibrate" onPress={() => setMode(RINGER_MODE.vibrate)} />
    </View>
  );
}
```

## Not allowed to change Do Not Disturb state: checkDndAccess & requestDndAccess

Starting from Android N, changing the ringer mode to a state that would enable Do Not Disturb is not allowed unless the app has been granted Do Not Disturb Access. See [AudioManager#setRingerMode](<https://developer.android.com/reference/android/media/AudioManager#setRingerMode(int)>).

If you want to change the ringer mode from Silent mode or to Silent mode, you may encounter the "Not allowed to change Do Not Disturb state" error. The example below shows how to check for DND access, and if the user hasn't granted access, how to prompt them to open the settings:

First, add the following line to your `AndroidManifest.xml` file to enable your app to appear in the settings.

```xml
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
```

And you can check and request permission before setting the ringer mode. Example code below:

```tsx
import React from 'react';
import { View, Button } from 'react-native';

import {
  useRingerMode,
  RINGER_MODE,
  checkDndAccess,
  requestDndAccess,
  RingerModeType,
} from 'react-native-volume-manager';

export default function App() {
  const { mode, setMode } = useRingerMode();

  const changeMode = async (newMode: RingerModeType) => {
    // From N onward, ringer mode adjustments that would toggle Do Not Disturb
    // are not allowed unless the app has been granted Do Not Disturb Access.
    // @see https://developer.android.com/reference/android/media/AudioManager#setRingerMode(int)
    if (newMode === RINGER_MODE.silent || mode === RINGER_MODE.silent) {
      const hasDndAccess = await checkDndAccess();
      if (hasDndAccess === false) {
        // This function opens the DND settings.
        // You can ask user to give the permission with a modal before calling this function.
        requestDndAccess();
        return;
      }
    }

    setMode(newMode);
  };

  return (
    <View>
      <Button title="Silent" onPress={() => changeMode(RINGER_MODE.silent)} />
      <Button title="Normal" onPress={() => changeMode(RINGER_MODE.normal)} />
      <Button title="Vibrate" onPress={() => changeMode(RINGER_MODE.vibrate)} />
    </View>
  );
}
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## Special thanks

- Uses code from https://github.com/c19354837/react-native-system-setting
- Uses code from https://github.com/zmxv/react-native-sound
- Uses code from https://github.com/vitorverasm/react-native-silent
- Uses code from https://github.com/GeorgyMishin/react-native-silent-listener
- Fully implements https://github.com/reyhankaplan/react-native-ringer-mode

I used parts, or even the full source code, of these libraries (with plenty of adjustments and rewrites to TypeScript) to make this library work on Android and iOS and to have a mostly unified API that handles everything related to volume. Since many of the packages I found were unmaintained or abandoned and only solved some of the issues, I decided to create my own. I hope you find it useful!

## License

MIT
