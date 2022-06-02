# react-native-volume-manager

Adds the ability to change the system volume on iOS and Android, listen to volume changes and supress the native volume UI to build your own volume slider or UX.

## Installation

```sh
npm install react-native-volume-manager
```

or

```sh
yarn add react-native-volume-manager
```

## Quick usage overview

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
const { volume } = await VolumeManager.getVolume(type: 'music');

// or the oldschool way
VolumeManager.getVolume.then((result) => {
  console.log(result.volume); // returns the current volume as a float (0-1)
});


useEffect(() => {
  // listen to volume changes
  const volumeListener = VolumeManager.addListener((result) => {
    console.log(result.volume); // returns the current volume as a float (0-1)
  });

  return function () {
      // remove listener
      volumeListener.remove();
  }
})

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

## Thanks

Based on https://github.com/c19354837/react-native-system-setting, rewritten to TS and optimized for current React Native versions.
