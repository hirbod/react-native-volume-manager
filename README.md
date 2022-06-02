# react-native-volume-manager

Adds the ability to change the system volume on iOS and Android, listen to volume changes and supress the native volume UI to build your own volume slider or UX.

## Installation

```sh
npm install react-native-volume-manager

or

yarn add react-native-volume-manager
```

## API usage

```tsx
import VolumeManager from 'react-native-volume-manager';

// ...

// set volume
VolumeManager.setVolume(0.5);

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

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
