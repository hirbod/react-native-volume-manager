![React Native Volume Manager Ringer Mute Silent Switch](gh-banner.png)

# react-native-volume-manager

Enhance system volume control on iOS and Android with this native package. Adjust the volume, monitor changes, and create custom volume sliders and user experiences. The package provides an intuitive API for accessing the current volume level, detecting the silent switch status (iOS), and tracking ringer mode changes (Android).

## Features

- Adjust system volume
- Monitor volume changes
- Suppress default volume UI
- Access current volume level
- Detect silent switch status (iOS)
- Track ringer mode changes (Android)

## Installation

Using npm:

```sh
npm install react-native-volume-manager
```

Using Yarn:

```sh
yarn add react-native-volume-manager
```

For React Native >= 0.60, manual linking is not required with Autolinking.

> Note: This library is incompatible with Expo Go. To use it, you can install a custom development client as recommended in 2022.

## Usage

All methods are available under the `VolumeManager` namespace or can be imported directly. Here are some examples:

```tsx
import { VolumeManager } from 'react-native-volume-manager';

// Disable the native volume toast globally (iOS, Android)
VolumeManager.showNativeVolumeUI({ enabled: true });

// Set the volume (value between 0 and 1)
await VolumeManager.setVolume(0.5);

// Get the current volume async
const { volume } = await VolumeManager.getVolume();

// Listen to volume changes
const volumeListener = VolumeManager.addVolumeListener((result) => {
  console.log(result.volume);

  // On Android, additional volume types are available:
  // music, system, ring, alarm, notification
});

// Remove the volume listener
volumeListener.remove();
```

For more usage examples and detailed API documentation, please refer to the [GitHub repository](https://github.com/your-repo).

## iOS Audio Session Management

This section provides methods related to AVAudioSession on iOS. For example:

```tsx
import { VolumeManager } from 'react-native-volume-manager';

// Enable or disable iOS AudioSession
VolumeManager.enable(true, true); // Enable async
VolumeManager.enable(false, true); // Disable async, non-blocking

// Activate or deactivate the audio session
VolumeManager.setActive(true, true); // Activate async
VolumeManager.setActive(false, true); // Deactivate async, non-blocking
```

## iOS Mute Switch Listener

To monitor the mute switch status on iOS, you can use the following:

```tsx
import { VolumeManager } from 'react-native-volume-manager';

const silentListener = VolumeManager.addSilentListener((status) => {
  console.log(status.isMuted);
  console.log(status.initialQuery);
});

// Remove the silent listener
silentListener.remove();
```

## Android Ringer Listener

To listen to ringer mode changes on Android, you can use the following:

```tsx
import { VolumeManager } from 'react-native-volume-manager';

const ringerListener = VolumeManager.addRingerListener((status) => {
  console.log(status.ringerMode);
});

// Remove the ringer listener
VolumeManager.removeRingerListener(ringerListener);
```

## Hooks / Ringer Mode (Android only)

You can use the `useRingerMode` hook to get and set the ringer mode on Android:

```tsx
import React from 'react';
import { View, Text, Button } from 'react-native';
import {
  useRingerMode,
  RINGER_MODE,
  RingerModeType,
} from 'react-native-volume-manager';

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

## API

Here are some of the available methods in the `VolumeManager` API:

- `showNativeVolumeUI(config: { enabled: boolean }): void`
- `getVolume(): Promise<VolumeResult>`
- `setVolume(value: number, config?: object): Promise<void>`
- `addVolumeListener(callback): void`
- `enableInSilenceMode(value: boolean): void`
- `setCategory(value: AVAudioSessionCategory, mixWithOthers?: boolean): void`
- `setMode(mode: AVAudioSessionMode): void`
- `addRingerListener(callback): void`
- `removeRingerListener(listener): void`
- `isRingerListenerEnabled(): boolean`
- `getRingerMode(): Promise<RingerModeType>`
- `setRingerMode(mode: RingerModeType): Promise<void>`
- `requestDndAccess(): void`
- `checkDndAccess(): Promise<boolean>`

For detailed API documentation and additional examples, please refer to the [GitHub repository](https://github.com/your-repo).

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

MIT
