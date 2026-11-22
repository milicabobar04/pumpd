import React, { useRef, useState } from 'react';
import { View, Text, ScrollView, ActivityIndicator, StyleSheet } from 'react-native';
import { CameraView, useCameraPermissions } from 'expo-camera';
import { useIsFocused } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import { Button, ErrorMessage, FormField } from '../../components';
import { theme } from '../../theme';
import { registerEntry, registerExit } from '../../services/attendanceService';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;
const CAMERA_SIZE = 260;

const MODES = {
  entry: { label: 'Ulazak', send: registerEntry },
  exit: { label: 'Izlazak', send: registerExit },
};

// '2026-11-21T15:45' -> '15:45'
const formatTime = (value) => (value ? String(value).slice(11, 16) : '');

function resultFromError(e, mode) {
  const status = e.response?.status;
  if (status === 404) {
    return { ok: false, title: 'Nepoznat QR kod', message: 'Ovaj kod ne pripada nijednom članu.' };
  }
  if (status === 409) {
    return mode === 'entry'
      ? { ok: false, title: 'Član je već u teretani', message: 'Ulazak je danas već evidentiran.' }
      : { ok: false, title: 'Član nije u teretani', message: 'Za ovog člana danas nije evidentiran ulazak.' };
  }
  return null; 
}

export default function ScanQrScreen() {
  const [permission, requestPermission] = useCameraPermissions();
  const isFocused = useIsFocused();

  const [mode, setMode] = useState('entry');
  const [manualCode, setManualCode] = useState('');
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState(null); // { ok, title, message }
  const [error, setError] = useState(null);
  const locked = useRef(false);

  const submit = async (code) => {
    const qrCode = String(code ?? '').trim();
    if (!qrCode || locked.current) return;
    locked.current = true;
    setSending(true);
    setError(null);
    try {
      const data = await MODES[mode].send(qrCode);
      if (mode === 'entry') {
        setResult({
          ok: data.isAllowed,
          title: data.memberName,
          message: data.isAllowed ? 'Ulazak je evidentiran.' : data.message,
        });
      } else {
        setResult({ ok: true, title: data.memberName, message: `Izlazak je evidentiran u ${formatTime(data.exitTime)}.` });
      }
    } catch (e) {
      const known = resultFromError(e, mode);
      if (known) {
        setResult(known);
      } else {
        setError(e);
        locked.current = false; 
      }
    } finally {
      setSending(false);
    }
  };

  const next = () => {
    setResult(null);
    setManualCode('');
    setError(null);
    locked.current = false;
  };

  const changeMode = (key) => {
    setMode(key);
    next();
  };

  let scanner;
  if (result) {
    scanner = (
      <View style={[styles.result, result.ok ? styles.resultOk : styles.resultDenied]} accessibilityLiveRegion="polite">
        <Ionicons name={result.ok ? 'checkmark-circle' : 'close-circle'} size={56}
          color={result.ok ? colors.primary : colors.error} />
        <Text style={styles.resultTitle} maxFontSizeMultiplier={MAX_FONT_SCALE}>{result.title}</Text>
        <Text style={styles.resultMessage} maxFontSizeMultiplier={MAX_FONT_SCALE}>{result.message}</Text>
        <Button title="Skeniraj sljedećeg" variant="action" onPress={next} />
      </View>
    );
  } else if (!permission) {
    scanner = <ActivityIndicator size="large" color={colors.primary} />;
  } else if (!permission.granted) {
    scanner = (
      <View style={styles.permission}>
        <Text style={styles.hint} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {permission.canAskAgain
            ? 'Za skeniranje je potrebna dozvola za kameru.'
            : 'Dozvolu za kameru uključite u podešavanjima telefona.'}
        </Text>
        {permission.canAskAgain ? (
          <Button title="Dozvoli kameru" variant="action" onPress={requestPermission} />
        ) : null}
      </View>
    );
  } else {
    scanner = (
      <View style={styles.cameraBox}>
        {isFocused ? (
          <CameraView
            style={StyleSheet.absoluteFill}
            facing="back"
            barcodeScannerSettings={{ barcodeTypes: ['qr'] }}
            onBarcodeScanned={sending ? undefined : ({ data }) => submit(data)}
          />
        ) : null}
        {sending ? <ActivityIndicator size="large" color={colors.primary} /> : null}
      </View>
    );
  }

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
      <View style={styles.modes} accessibilityRole="radiogroup">
        {Object.entries(MODES).map(([key, { label }]) => (
          <Button key={key} title={label} variant={mode === key ? 'confirm' : 'secondary'}
            onPress={() => changeMode(key)} disabled={sending} />
        ))}
      </View>

      {scanner}
      <ErrorMessage error={error} inline />

      <FormField
        label="Ili unesite kod ručno"
        placeholder="npr. SEED-QR-CLAN1"
        value={manualCode}
        onChangeText={setManualCode}
        autoCapitalize="characters"
        autoCorrect={false}
        editable={!sending && !result}
        returnKeyType="send"
        onSubmitEditing={() => submit(manualCode)}
      />
      <Button
        title={`Evidentiraj ${MODES[mode].label.toLowerCase()}`}
        variant="action"
        onPress={() => submit(manualCode)}
        loading={sending}
        disabled={!manualCode.trim() || !!result}
        style={styles.centerButton}
      />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  content: { flexGrow: 1, padding: spacing.px24, gap: spacing.px16, alignItems: 'stretch' },
  modes: { flexDirection: 'row', justifyContent: 'center', gap: spacing.px12 },
  cameraBox: {
    width: CAMERA_SIZE,
    height: CAMERA_SIZE,
    alignSelf: 'center',
    overflow: 'hidden',
    borderRadius: borderRadius.px10,
    borderWidth: 2,
    borderColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.surface,
  },
  permission: { alignItems: 'center', gap: spacing.px12 },
  hint: { ...typography.body, textAlign: 'center' },
  result: { alignItems: 'center', gap: spacing.px8, padding: spacing.px16, borderRadius: borderRadius.px10, borderWidth: 2 },
  resultOk: { borderColor: colors.primary },
  resultDenied: { borderColor: colors.error },
  resultTitle: { ...typography.sectionTitle, textAlign: 'center' },
  resultMessage: { ...typography.body, textAlign: 'center' },
  centerButton: { alignSelf: 'center' },
});