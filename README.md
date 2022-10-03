![React Native Volume Manager Ringer Mute Silent Switch](gh-banner.png)

# react-native-volume-manager

This native package adds the ability to change the system volume on iOS and Android, listen to volume changes and suppress the native volume UI to build your own volume slider and UX. It also provides a simple API to get the current volume level. On iOS, you can check if the silent switch is enabled and listen to changes. On Android, you can set and listen for ringer mode changes.

| ![React Native Volume Manager](ios-preview.gif) | ![React Native Volume Manager](android-preview.gif) |
| ----------------------------------------------- | --------------------------------------------------- |

## Notice

This library does **not work** in a **Simulator** or **Emulator**. It is only working on a real device.

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

This library adds native code. It does not work with _Expo Go_ but you can easily install it using a [custom dev client](https://docs.expo.dev/development/getting-started/). Thats how it should be done in 2022 :).

**No config plugin required.**

---

## Usage 🚀

All methods are available under the `VolumeManager` namespace or can be imported directly.
(eg. `addVolumeListener`)

```tsx
import { VolumeManager } from 'react-native-volume-manager';

// ...

// show the native volume toast globally (iOS, Android)
// Warning: using setVolume() will always reset to the setting passed via showUI on iOS.
// Make sure to re-call this function if you changed between the values. Check out the example app.
VolumeManager.showNativeVolumeUI({ enabled: true}); // default is true

// set the volume, value between 0 and 1 (float)
await VolumeManager.setVolume(0.5); // float value between 0 and 1

// set volume with extra options
await VolumeManager.setVolume(0.5, {
  // defaults to "music" (Android only)
  type: 'system',

  // defaults to false, can surpress the native UI Volume Toast (iOS & Android)
  showUI: true,

  // defaults to false (Android only)
  playSound: false,
});

// Get the current volume async, type defaults to "music"
// (Android only, iOS only has one type of Volume)
// see down below for more types
// if you don't add a type, you'll get an object as a return on
// Android with all Volume types
const { volume } = await VolumeManager.getVolume(type: 'music');

// The oldschool way
VolumeManager.getVolume('music').then((result) => {

  // can be a number or object (depends on iOS or Android and if supplied for type)
  console.log(result);

  // NOTE: if you don't supply a type to getVolume on Android,
  //you will receive the VolumeResult object:

  /*
  {
    volume: number, // these are the same
    music: number, // these are the same
    system: number, // these are the same
    ring: number,
    alarm: number,
    notification: number,
  }
  */

  // iOS will always return only the volume as float
});

// listen to volume changes (example)
useEffect(() => {
  const volumeListener = VolumeManager.addVolumeListener((result) => {
     // returns the current volume as a float (0-1)
    console.log(result.volume);

    // on android, the result object will also have the keys
    // music, system, ring, alarm, notification
  });

  // clean up function
  return function () {
    // remove listener, just call .remove on the volumeListener
    // EventSubscription. Never forget to clean up your listeners.
    volumeListener.remove();
  }
}, []);

// or with useFocusEffect from react-navigation
useFocusEffect(
  React.useCallback(() => {
    const volumeListener = VolumeManager.addVolumeListener(async (result) => {
      if (Platform.OS === 'ios') {
        try {
          // once we detected a user on iOS triggered volume change,
          // we can change the audio mode to allow playback even when the
          // silent switch is activated. This can help to achieve effects like in
          // instragram reels, where videos are muted on silent switch
          // This example requires expo-av
          await Audio.setAudioModeAsync({
            playsInSilentModeIOS: true,
          });
        } catch {}
      }
    });
    return function blur() {
      // remove listener, just call .remove on the emitter
      volumeListener.remove();
    };
  }, [])
);
```

## iOS Audio Session Management

AVAudioSession related functions.

```tsx
import { VolumeManager } from 'react-native-volume-manager';

// first parameter is boolean to enable or disable, second if it should happen async (non-blocking) or sync (blocking). Second param is true by default

// enable iOS Audiosession (usually happening automatically when playing audio, this one activates Ambient mode)
VolumeManager.enable(true, true); // second parameter true = async
// disable iOS audiosession
VolumeManager.enable(false, true); // second parameter true = async

// if you want to activate or deactivate the audio session and inform running background music to resume, call setActive()

// Activate audio session
// for example when you background the app
VolumeManager.setActive(true, true); // second parameter true = async

// Deactivate audio session, inform background music apps to resume automatically
VolumeManager.setActive(false, true); // second parameter true = async, non-blocking

// This method triggers `AVAudioSessionSetActiveOptionNotifyOthersOnDeactivation` automatically.

// Example, explicitly not triggering when status is "inactive"
// only when active or background. Storing result in a ref to prevent
// spamming the UI/JS thread
const onAppStateChange = useCallback(async (status: AppStateStatus) => {
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
useAppState({
  onChange: onAppStateChange,
});
```

If you want to change the Audio Category or Audio Mode on iOS, or play music when the silent switch is active,
you can call the functions `enableInSilenceMode`, `setCategory` or `setMode`. Please refefer to Apples documentation to find out what they do.

```tsx
import { VolumeManager } from 'react-native-volume-manager';

// Change mode, available options are typed
// Default, VoiceChat, VideoChat, GameChat, VideoRecording, Measurement, MoviePlayback, SpokenAudio

VolumeManager.setMode(mode)

// Change category
// Ambient, SoloAmbient, Playback, Record, PlayAndRecord, MultiRoute, Alarm
VolumeManager.setCategory(value: AVAudioSessionCategory, mixWithOthers?: boolean) // 2nd param defaults to false

// enable playing audio when silent switch is active
VolumeManager.enableInSilenceMode(true)

// disable playing audio when silent switch is active
VolumeManager.enableInSilenceMode(false)
```

## iOS mute switch listener

There is no native iOS API to detect if the mute switch is enabled/disabled on a device.

The general principle to check if the device is muted is to play a short sound without audio and detect the length it took to play. **Has a trigger rate of 1 second**.

**Note: The check is performed on the native main thread, not the JS thread.**

You can increase or decrease how often the check is performed by changing the `VolumeManager.setNativeSilenceCheckInterval(1)` property.
Minimum value is `0.5`, default is `2`. The default value is usually enough.

```tsx
import { VolumeManager } from 'react-native-volume-manager';
const [isSilent, setIsSilent] = useState<boolean>();
const [initialQuery, setInitialQuery] = useState<boolean>();

// optional, default is 2 seconds. You can choose to set a higher value
// when fast recognition is not critical (will save Battery)
VolumeManager.setNativeSilenceCheckInterval(1); // min 0.5, default 2

// ....
// ....

useEffect(() => {
  const silentListener = VolumeManager.addSilentListener((status) => {
    setIsSilent(status.isMuted);
    setInitialQuery(status.initialQuery);
  });

  return () => {
    // remove listener, just call .remove on the emitter return
    // never forget to clean up
    silentListener.remove();
  };
}, []);
```

You can also use the hook.

```tsx
import { useSilentSwitch } from 'react-native-volume-manager';

//....
const { isMuted, initialQuery } = useSilentSwitch();

// or with parameter which controls which
// interval the native thread should look for changes. 0.5 is min
// defaults to 2 (higher values are even recommened since it will drain less battery)

const { isMuted, initialQuery } = useSilentSwitch(1);

// returns boolean on iOS and undefined on any other platform.
```

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

setRingerMode is an async function that sets the given ringer mode to the device and resolves the mode if it is set. (Resolves undefined on non-Android devices.)

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

## Not allowed to change `Do Not Disturb state` checkDndAccess & requestDndAccess

From N onward, ringer mode adjustments that would toggle Do Not Disturb are not allowed unless the app has been granted Do Not Disturb Access. See [AudioManager#setRingerMode](<https://developer.android.com/reference/android/media/AudioManager#setRingerMode(int)>).

If you want to change the ringer mode from Silent mode or to Silent mode, you may run into the Not allowed to change Do Not Disturb state error. The example below checks the DND access and if user hasn't given the access opens the settings for it.

First you need to add the line below to your AndroidManifest.xml to be able to see your app in the settings.

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

I used parts or even the full source code of these libraries (with plenty of adjustments and rewirtes to TS) to make this library work on Android and iOS and to have a mostly unified API which does everything related to Volume. Since most of the packages I've found have been unmaintained or abandoned and also only solved parts of the issues, I decided to make my own. I hope you like it!

## License

MIT
