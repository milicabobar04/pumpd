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
      {/* Lijeva i desna strana imaju istu širinu, pa je logo tačno u sredini (kao u AppHeader) */}
      <View style={styles.side}>
        <Pressable
          onPress={onBack}
          hitSlop={HIT_SLOP}
          accessibilityRole="button"
          accessibilityLabel="Nazad"
          style={({ pressed }) => pressed && styles.pressed}
        >
          <Ionicons name="arrow-back" size={28} color={colors.primary} />
        </Pressable>
      </View>

      <Logo height={36} />

      <View style={styles.side} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.px16,
    paddingBottom: spacing.px8,
    backgroundColor: colors.background,
  },
  side: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  },
  pressed: {
    opacity: 0.6,
  },
});
