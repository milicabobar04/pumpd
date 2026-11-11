import React from 'react';
import { TouchableOpacity, Text, ActivityIndicator, StyleSheet } from 'react-native';
import { buttons } from '../theme/Buttons';

// Slova se mogu povećati iz podešavanja telefona, ali najviše 1.3x,
// da tekst ne bi razbio raspored dugmadi.
const MAX_FONT_SCALE = 1.3;

export const Button = ({
  title,
  onPress,
  variant = 'primary',
  loading = false,
  disabled = false,
  style,
  textStyle,
}) => {
  const selected = buttons[variant] || buttons.primary;
  const isDisabled = disabled || loading;

  return (
    <TouchableOpacity
      // `style` je zadnji, pa na ekranu može pregaziti veličinu varijante
      // (npr. style={{ width: 'auto' }} ili style={{ alignSelf: 'stretch' }}).
      style={[selected.container, disabled && { opacity: 0.6 }, style]}
      onPress={onPress}
      disabled={isDisabled}
      activeOpacity={0.8}
      hitSlop={selected.hitSlop}
      accessibilityRole="button"
      accessibilityLabel={title}
      accessibilityState={{ disabled: isDisabled, busy: loading }}
    >
      {loading ? (
        <ActivityIndicator color={selected.label.color} />
      ) : (
        <Text style={[styles.text, selected.label, textStyle]} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {title}
        </Text>
      )}
    </TouchableOpacity>
  );
};

const styles = StyleSheet.create({
  // Android dodaje prazan prostor iznad i ispod slova (font padding),
  // pa tekst ne bi bio vertikalno centriran kao na iOS-u i u Figmi.
  text: {
    includeFontPadding: false,
    textAlignVertical: 'center',
    textAlign: 'center',
  },
});
