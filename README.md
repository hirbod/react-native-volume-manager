![Drag Racing](gh-banner.png)

# react-native-volume-manager

This native package adds the ability to change the system volume on iOS and Android, listen to volume changes and suppress the native volume UI to build your own volume slider and UX. It also provides a simple API to get the current volume level.

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

## Usage 🚀

```tsx
import VolumeManager from 'react-native-volume-manager';

// ...

// set volume
VolumeManager.setVolume(0.5); // float value between 0 and 1

// set volume with extra options
VolumeManager.setVolume(0.5, {
  type: 'system', // defaults to "music" (Android only)
  showUI: true, // defaults to false, can surpress the native UI Volume Toast (iOS & Android)
  playSound: false, // defaults to false (when pushing hardware buttons) (Android only)
});

// get volume async, type defaults to "music" (Android only)
// iOS has only one type of Volume (system)
// see down below for more types
const { volume } = await VolumeManager.getVolume(type: 'music');

// or the oldschool way
VolumeManager.getVolume.then((result) => {
  console.log(result.volume); // returns the current volume as a float (0-1)
});

// listen to volume changes
useEffect(() => {
  const volumeListener = VolumeManager.addListener((result) => {
    console.log(result.volume); // returns the current volume as a float (0-1)
  });

  // clean up function
  return function () {
      // remove listener
      volumeListener.remove();
  }
}, []);

// or with useFocusEffect from react-navigation
useFocusEffect(
  React.useCallback(() => {
    const volumeListener = VolumeManager.addListener(async (data) => {
      if (Platform.OS === 'ios') {
        try {
          // once we detected a user on iOS triggered volume change,
          // we can change the audio mode to allow playback even when the
          // silent switch is activated.
          // This example requires expo-av
          await Audio.setAudioModeAsync({
            playsInSilentModeIOS: true,
          });
        } catch {}
      }
    });
    return function blur() {
      volumeListener.remove();
    };
  }, [])
);
```

## API

| Method                                | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                               |
| ------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Volume**                            |
| getVolume(type:string) => Promise     | Get the system volume. <br><br>`type` must be one of `music`, `call`, `system`, `ring`, `alarm`, `notification`, default is `music`. (Android only, iOS will always report the system volume)                                                                                                                                                                                                                                                                                             |
| setVolume(value:float, config:object) | Set the system volume by specified value, from 0 to 1. 0 for mute, and 1 for the max volume.<br><br> `config` can be `{type: 'music', playSound:true, showUI:true}`<br><br> `type` : must be one of `music`, `call`, `system`, `ring`, `alarm`, `notification`, default is `music`. (Android only) <br>`playSound`: Whether to play a sound when changing the volume, default is `false` (Android only)<br>`showUI`: Show the native system volume UI, default is `false` (Android & iOS) |
| addListener(callback)                 | Listen to volume changes (soft- and hardware. addListener will return the listener which is needed for cleanup. Result passed to callback contains `volume` as key.                                                                                                                                                                                                                                                                                                                       |
| `listener.remove()`                   | Remove the listener when you don't need it anymore. Store the return of `const listener = VolumeManager.addListener()` in a variable and call it with `.remove()`. See the example above.                                                                                                                                                                                                                                                                                                 |

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT

## Special thanks

Based on https://github.com/c19354837/react-native-system-setting, rewritten to TS and optimized for current React Native versions.
