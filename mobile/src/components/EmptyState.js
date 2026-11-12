import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { theme } from '../theme';
import { Button } from './Button';

const { colors, spacing, typography } = theme;
const MAX_FONT_SCALE = 1.3;

// Prikaz prazne liste: ikona, naslov, kratko objašnjenje i po želji dugme.
//
// U FlatList ide kao ListEmptyComponent. Da bi bio centriran po visini,
// listi dodaj contentContainerStyle={{ flexGrow: 1 }}.
//
// <FlatList
//   data={grupe}
//   ListEmptyComponent={
//     <EmptyState title="Nema grupa" message="Još nisi dodala nijednu grupu." />
//   }
//   contentContainerStyle={{ flexGrow: 1 }}
// />
export const EmptyState = ({
  icon = 'file-tray-outline',
  title = 'Nema stavki',
  message,
  actionLabel,
  onAction,
  style,
}) => (
  <View style={[styles.container, style]}>
    <Ionicons name={icon} size={56} color={colors.iconMuted} />

    <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
      {title}
    </Text>

    {message ? (
      <Text style={styles.message} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        {message}
      </Text>
    ) : null}

    {actionLabel && onAction ? (
      <Button title={actionLabel} onPress={onAction} variant="add" style={styles.action} />
    ) : null}
  </View>
);

const styles = StyleSheet.create({
  container: {
    flexGrow: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.px12,
    padding: spacing.px24,
  },
  title: {
    ...typography.sectionTitle,
    textAlign: 'center',
  },
  message: {
    ...typography.body,
    color: colors.textSecondary,
    textAlign: 'center',
    maxWidth: 300,
  },
  action: {
    marginTop: spacing.px12,
  },
});

export default EmptyState;
