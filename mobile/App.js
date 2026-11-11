import React from 'react';
import { StyleSheet, View, Text } from 'react-native';

import { Button } from './src/components/Button';

import { theme, useAppFonts } from './src/theme';

export default function App() {
  const fontsReady = useAppFonts();
  if (!fontsReady) return null;
  return (
    <View style={styles.container}>
      <Text style={styles.title}>PumpD - Test Button Komponente</Text>

      {/* Test glavnog dugmeta */}
      <Button
        title="Sign In"
        variant="login"
        onPress={() => alert('Kliknuto login dugme!')}
      />

      {/* Test akcionog dugmeta */}
      <Button
        title="Prijavi problem"
        variant="action"
        onPress={() => alert('Kliknuto akciono dugme!')}
      />

      {/* Test chip dugmeta */}
      <Button
        title="Recenzije"
        variant="chip"
        onPress={() => alert('Kliknut chip!')}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.background,
    padding: theme.spacing.px24,
    gap: theme.spacing.px16,
    justifyContent: 'center',
    alignItems: 'center',
  },
  title: {
    ...theme.typography.sectionTitle,
    color: theme.colors.primary,
    textAlign: 'center',
    marginBottom: theme.spacing.px24,
  },
});