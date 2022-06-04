import { useEffect, useState } from 'react';
import {
  StyleSheet,
  Text,
  View,
  Pressable,
  Button,
  Platform,
} from 'react-native';
import {
  VolumeManager,
  useRingerMode,
  RINGER_MODE,
} from 'react-native-volume-manager';
import Slider from '@react-native-community/slider';

const modeText = {
  [RINGER_MODE.silent]: 'Silent',
  [RINGER_MODE.normal]: 'Normal',
  [RINGER_MODE.vibrate]: 'Vibrate',
};

export default function App() {
  const [currentSystemVolume, setReportedSystemVolume] = useState<number>(0);
  const [isMuted, setIsMuted] = useState<boolean>();

  useEffect(() => {
    VolumeManager.getVolume('music').then((result) => {
      setReportedSystemVolume(result);
      console.log('Trying to read current volume', result);
    });

    const volumeListener = VolumeManager.addListener((result) => {
      setReportedSystemVolume(result.volume);
      console.log('Volume changed', result);
    });

    const silentListener = VolumeManager.addSilentListener((isMuted) => {
      setIsMuted(isMuted);
    });

    return () => {
      // remove
      volumeListener.remove();
      silentListener.remove();
    };
  }, []);

  const { mode, error, setMode } = useRingerMode();

  const setVolume = () => {
    VolumeManager.setVolume(0.1, { showUI: true });
  };

  return (
    <View style={styles.container}>
      <View>
        <Text style={styles.headline}>iOS / Android</Text>
      </View>
      <View style={styles.col}>
        <Text>Current volume:</Text>
        <Text>{currentSystemVolume}</Text>
      </View>
      <View style={styles.col}>
        <Text>Is muted?:</Text>
        <Text>{currentSystemVolume <= 0 ? 'YES' : 'NO'}</Text>
      </View>
      <View style={styles.col}>
        <Text>Silent switch active?:</Text>
        <Text>{isMuted ? 'YES' : 'NO'}</Text>
      </View>

      <View>
        <Text style={styles.headline2}>Volume update with native UI</Text>
      </View>
      <Slider
        style={{ width: '100%', height: 40 }}
        minimumValue={0}
        maximumValue={1}
        minimumTrackTintColor="#000"
        maximumTrackTintColor="#999"
        onValueChange={async (value) => {
          await VolumeManager.setVolume(value, { showUI: true });
          setReportedSystemVolume(value);
        }}
        value={currentSystemVolume}
      />

      <View>
        <Text style={styles.headline2}>Volume update with suppressed UI</Text>
      </View>
      <Slider
        style={{ width: '100%', height: 40 }}
        minimumValue={0}
        maximumValue={1}
        minimumTrackTintColor="#000"
        maximumTrackTintColor="#999"
        onValueChange={async (value) => {
          await VolumeManager.setVolume(value, { showUI: false });
          setReportedSystemVolume(value);
        }}
        value={currentSystemVolume}
      />

      <View>
        <Text style={styles.headline}>Android only features</Text>
      </View>

      <View style={styles.col}>
        <Text>Ringer Mode:</Text>
        <Text>
          {mode !== undefined
            ? modeText[mode]
            : Platform.OS === 'ios'
            ? 'Unsupported on iOS'
            : 'Unknown'}
        </Text>
      </View>
      <View style={styles.col}>
        <Text>Set Ringer mode:</Text>
        <View>
          <Button title="Silent" onPress={() => setMode(RINGER_MODE.silent)} />
          <Button title="Normal" onPress={() => setMode(RINGER_MODE.normal)} />
          <Button
            title="Vibrate"
            onPress={() => setMode(RINGER_MODE.vibrate)}
          />
        </View>
      </View>

      <View>
        <View />

        <View>
          <Text>{error?.message}</Text>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
    justifyContent: 'center',
    padding: 20,
  },
  col: {
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    flexDirection: 'row',
    marginVertical: 5,
  },
  headline: {
    fontSize: 20,
    fontWeight: 'bold',
    marginVertical: 20,
  },
  headline2: {
    fontSize: 16,
    fontWeight: 'bold',
    marginVertical: 20,
  },
});
