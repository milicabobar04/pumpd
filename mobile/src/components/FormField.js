import React, { forwardRef, useState } from 'react';
import { View, Text, TextInput, Pressable, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { theme } from '../theme';

const { colors, fonts, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;

// Polje forme: labela, unos i poruka o grešci ispod polja.
// Svi ostali props (value, onChangeText, placeholder, keyboardType,
// autoCapitalize, returnKeyType, onSubmitEditing...) idu direktno na TextInput.
// forwardRef omogućava prelazak na sljedeće polje: ref.current.focus().
//
// Napomena: izgled polja (pozadina, obrub, visina) nije preuzet iz Figme.
// Kad otvoriš polje za unos u Figmi (Inspect), uskladi styles.inputRow i styles.input.
export const FormField = forwardRef(function FormField(
  {
    label,
    error,
    secureTextEntry = false,
    editable = true,
    style,
    inputStyle,
    onFocus,
    onBlur,
    ...inputProps
  },
  ref,
) {
  const [focused, setFocused] = useState(false);
  const [hidden, setHidden] = useState(secureTextEntry);

  // Greška ima prednost nad fokusom, fokus nad običnim stanjem.
  const borderColor = error ? colors.error : focused ? colors.primary : colors.border;

  return (
    <View style={[styles.wrapper, style]}>
      {label ? (
        <Text style={styles.label} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {label}
        </Text>
      ) : null}

      <View style={[styles.inputRow, { borderColor }, !editable && styles.disabled]}>
        <TextInput
          ref={ref}
          style={[styles.input, inputStyle]}
          placeholderTextColor={colors.inputPlaceholder}
          selectionColor={colors.primary}
          cursorColor={colors.primary}
          secureTextEntry={hidden}
          editable={editable}
          maxFontSizeMultiplier={MAX_FONT_SCALE}
          accessibilityLabel={label}
          accessibilityHint={error || undefined}
          onFocus={(e) => {
            setFocused(true);
            onFocus?.(e);
          }}
          onBlur={(e) => {
            setFocused(false);
            onBlur?.(e);
          }}
          {...inputProps}
        />

        {/* Oko za prikaz lozinke, samo kad je polje za lozinku */}
        {secureTextEntry ? (
          <Pressable
            onPress={() => setHidden((h) => !h)}
            hitSlop={12}
            style={styles.eye}
            accessibilityRole="button"
            accessibilityLabel={hidden ? 'Prikaži lozinku' : 'Sakrij lozinku'}
          >
            <Ionicons
              name={hidden ? 'eye-outline' : 'eye-off-outline'}
              size={22}
              color={colors.iconMuted}
            />
          </Pressable>
        ) : null}
      </View>

      {error ? (
        <Text style={styles.error} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {error}
        </Text>
      ) : null}
    </View>
  );
});

const styles = StyleSheet.create({
  wrapper: {
    gap: spacing.px6,
  },
  label: {
    ...typography.label,
  },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: 48, // najmanja površina za dodir
    paddingHorizontal: spacing.px16,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderRadius: borderRadius.px8,
  },
  input: {
    flex: 1,
    paddingVertical: spacing.px12,
    fontFamily: fonts.inter400,
    fontSize: 16,
    color: colors.textPrimary,
    includeFontPadding: false,
  },
  eye: {
    marginLeft: spacing.px8,
  },
  error: {
    ...typography.error,
  },
  disabled: {
    opacity: 0.6,
  },
});

export default FormField;
