import { useEffect, useState } from 'react';
import { StyleSheet, Text, View, Button, Platform } from 'react-native';
import {
  VolumeManager,
  useRingerMode,
  RINGER_MODE,
  RingerSilentStatus,
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
  const [initialQuery, setInitialQuery] = useState<boolean>();
  const [ringerStatus, setRingerStatus] = useState<RingerSilentStatus>();

  useEffect(() => {
    VolumeManager.getVolume('music').then((result) => {
      setReportedSystemVolume(
        typeof result === 'object' ? result.volume : result
      );
      console.log('Read system volume', result);
    });

    const volumeListener = VolumeManager.addVolumeListener((result) => {
      setReportedSystemVolume(result.volume);
      console.log('Volume changed', result);
    });

    const silentListener = VolumeManager.addSilentListener((status) => {
      console.log(status);
      setIsMuted(status.isMuted);
      setInitialQuery(status.initialQuery);
    });

    const ringerListener = VolumeManager.addRingerListener((result) => {
      console.log('Ringer listener changed', result);
      setRingerStatus(result);
    });

    return () => {
      // remove
      volumeListener.remove();
      silentListener.remove();
      VolumeManager.removeRingerListener(ringerListener);
    };
  }, []);

  const { mode, error, setMode } = useRingerMode();

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
        <Text style={styles.headline}>iOS only features</Text>
      </View>
      <View style={styles.col}>
        <Text>Silent switch active?:</Text>
        <Text>
          {Platform.OS === 'ios'
            ? `${isMuted ? 'YES' : 'NO'} (initial query: ${
                initialQuery ? 'YES' : 'NO'
              })`
            : 'Unsupported on Android'}
        </Text>
      </View>

      <View>
        <Text style={styles.headline}>Android only features</Text>
      </View>

      <View style={styles.col}>
        <Text>Ringer Mode listener:</Text>
        <Text>{ringerStatus?.mode}</Text>
      </View>

      <View style={styles.col}>
        <Text>Selected Ringer Mode:</Text>
        <Text>
          {mode !== undefined
            ? modeText[mode]
            : Platform.OS === 'ios'
            ? 'Unsupported on iOS'
            : 'Unknown'}
        </Text>
      </View>
      <View style={{ marginTop: 20 }}>
        <Text>Set Ringer mode:</Text>
        <View
          style={{
            marginTop: 20,
            height: 120,
            justifyContent: 'space-between',
          }}
        >
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

        <View
          style={{
            marginTop: 10,
            justifyContent: 'center',
            alignItems: 'center',
          }}
        >
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
    marginVertical: 10,
  },
  headline2: {
    fontSize: 16,
    fontWeight: 'bold',
    marginVertical: 20,
  },
});
