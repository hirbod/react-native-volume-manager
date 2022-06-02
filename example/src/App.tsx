import * as React from 'react';

import VolumeManager from 'react-native-volume-manager';

export default function App() {
  React.useEffect(() => {
    VolumeManager.getVolume('music').then((result) => {
      console.log(result.volume);
    });
  }, []);

  return;
}
