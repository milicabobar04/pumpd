import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Button } from './Button';
import { theme } from '../theme';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;

export const AccountCreated = ({ title, fullName, email, temporaryPassword, onDone }) => (
  <View style={styles.container} accessibilityRole="alert" accessibilityLiveRegion="polite">
    <Ionicons name="checkmark-circle-outline" size={72} color={colors.primary} />
    <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
      {title}
    </Text>
    <Text style={styles.text} maxFontSizeMultiplier={MAX_FONT_SCALE}>
      {fullName}, {email}
    </Text>

    <View style={styles.card}>
      <Text style={styles.label} maxFontSizeMultiplier={MAX_FONT_SCALE}>Privremena lozinka</Text>
      <Text style={styles.password} selectable maxFontSizeMultiplier={MAX_FONT_SCALE}>
        {temporaryPassword}
      </Text>
    </View>

    <Text style={styles.hint} maxFontSizeMultiplier={MAX_FONT_SCALE}>
      Odštampajte lozinku ili je saopštite korisniku. Pri prvoj prijavi postaviće svoju lozinku. Ova lozinka se više neće prikazati.
    </Text>

    <Button title="Gotovo" variant="action" onPress={onDone} />
  </View>
);

const styles = StyleSheet.create({
    container: { flexGrow: 1, alignItems: 'center', justifyContent: 'center', gap: spacing.px16 },
    title: { ...typography.screenTitle, textAlign: 'center' },
    text: { ...typography.body, color: colors.textSecondary, textAlign: 'center' },
    card: {
        alignSelf: 'stretch',
        alignItems: 'center',
        gap: spacing.px8,
        padding: spacing.px16,
        borderRadius: borderRadius.px10,
        backgroundColor: colors.surface,
    },
    label: { ...typography.caption },
    password: { ...typography.sectionTitle, color: colors.primary,
        letterSpacing: 1
    },
    hint: { ...typography.caption, textAlign: 'center' },
});

export default AccountCreated;