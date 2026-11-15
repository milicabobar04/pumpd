import React from 'react';
import { View, Pressable, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { Logo } from '../../components';
import { theme } from '../../theme';

const { colors, spacing } = theme;
const HIT_SLOP = { top: 12, bottom: 12, left: 12, right: 12 };

export default function PublicHeader({ onBack }) {
  const insets = useSafeAreaInsets();

  return (
    <View style={[styles.container, { paddingTop: insets.top + spacing.px8 }]}>
      <Pressable
        onPress={onBack}
        hitSlop={HIT_SLOP}
        accessibilityRole="button"
        accessibilityLabel="Nazad"
        style={({ pressed }) => pressed && styles.pressed}
      >
        <Ionicons name="arrow-back" size={28} color={colors.primary} />
      </Pressable>
      <Logo height={32} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.px16,
    paddingHorizontal: spacing.px16,
    paddingBottom: spacing.px8,
    backgroundColor: colors.background,
  },
  pressed: {
    opacity: 0.6,
  },
});
