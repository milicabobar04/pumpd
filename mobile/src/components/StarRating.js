import React from 'react';
import { View, Pressable, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { theme } from '../theme';

const { colors, spacing } = theme;
const MAX = 5;

// Zvjezdice za ocjenu 1–5.
//
// Prikaz:  <StarRating value={4} />
// Izbor:   <StarRating value={grade} onChange={setGrade} size={40} />
//          (kad je zadat onChange, svaka zvjezdica je dugme)
export const StarRating = ({
  value = 0,
  onChange,
  size = 18,
  color = colors.primary,
  emptyColor = colors.iconMuted,
  disabled = false,
  style,
}) => {
  const rounded = Math.round(Number(value) || 0);

  if (!onChange) {
    return (
      <View
        style={[styles.row, style]}
        accessible
        accessibilityLabel={`Ocjena ${rounded} od ${MAX}`}
      >
        {Array.from({ length: MAX }, (_, i) => (
          <Ionicons
            key={i}
            name={i < rounded ? 'star' : 'star-outline'}
            size={size}
            color={i < rounded ? color : emptyColor}
          />
        ))}
      </View>
    );
  }

  return (
    <View style={[styles.row, styles.pickerRow, style]} accessibilityRole="radiogroup">
      {Array.from({ length: MAX }, (_, i) => {
        const grade = i + 1;
        const filled = grade <= rounded;
        return (
          <Pressable
            key={grade}
            onPress={() => onChange(grade)}
            disabled={disabled}
            hitSlop={6}
            style={({ pressed }) => [pressed && styles.pressed, disabled && styles.disabled]}
            accessibilityRole="radio"
            accessibilityLabel={`${grade} od ${MAX}`}
            accessibilityState={{ checked: grade === rounded, disabled }}
          >
            <Ionicons name={filled ? 'star' : 'star-outline'} size={size} color={filled ? color : emptyColor} />
          </Pressable>
        );
      })}
    </View>
  );
};

const styles = StyleSheet.create({
  row: { flexDirection: 'row', alignItems: 'center', gap: spacing.px4 },
  pickerRow: { gap: spacing.px12 },
  pressed: { opacity: 0.7 },
  disabled: { opacity: 0.6 },
});

export default StarRating;
