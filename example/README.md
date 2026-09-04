# React Native Volume Manager example

This example uses Expo SDK 58 canary and React Native 0.87. Run these commands
from the `example` directory. SDK 58 needs an explicit canary template until its
stable template is published.

```sh
yarn
npx expo prebuild --clean --template expo-template-bare-minimum@58.0.0-canary-20260902-26df09e
npx expo run:ios
npx expo run:android
```

Use a native build to test volume controls. Expo Go does not include this module.

Slider is pinned to 5.2.1 and excluded from Expo's version check because 5.2.0
uses the `NativeMethods` type removed in React Native 0.87.
Safe-area-context is pinned to 5.9.1 and excluded because 5.7.0 uses the removed
Android `UIManagerModule.uiImplementation` API.
