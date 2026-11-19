import React, { forwardRef, useState } from 'react';
import { View, Text, TextInput, Pressable, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { theme } from '../theme';

const { colors, fonts, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;

// Visina polja iz Figme
const FIELD_HEIGHT = 42;

// Polje forme: labela, unos i poruka o grešci ispod polja.
// Ostali props (value, onChangeText, placeholder, keyboardType...) idu na TextInput.
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

  // Greška ima prednost nad fokusom.
  const borderColor = error ? colors.error : focused ? colors.primary : colors.border;
  // Polje u više redova (npr. opis problema) raste u visinu, pa ne smije imati fiksnu visinu.
  const multiline = inputProps.multiline === true;

  return (
    <View style={[styles.wrapper, style]}>
      {label ? (
        <Text style={styles.label} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {label}
        </Text>
      ) : null}

      <View
        style={[
          styles.inputRow,
          multiline && styles.inputRowMultiline,
          { borderColor },
          !editable && styles.disabled,
        ]}
      >
        <TextInput
          ref={ref}
          style={[styles.input, multiline && styles.inputMultiline, inputStyle]}
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
              color={colors.inputPlaceholder}
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
    gap: spacing.px8,
  },

  // Figma: Actor 18 / 25.2, zelena
  label: {
    ...typography.label,
    lineHeight: 25.2,
    color: colors.primary,
    includeFontPadding: false,
  },

  // Visina kao u Figmi; tekst se centrira sam, bez paddingVertical.
  // minHeight umjesto height, da polje u više redova može da raste.
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    minHeight: FIELD_HEIGHT,
    paddingHorizontal: spacing.px16,
    backgroundColor: colors.surface,
    borderWidth: 1,
    borderRadius: borderRadius.px8,
  },

  // Bez lineHeight: na TextInput-u zna odsjeći slova ili pomjeriti kursor.
  input: {
    flex: 1,
    minHeight: FIELD_HEIGHT - 2, // cijela visina reda bez obruba (2 x 1 px), da je cijelo polje za dodir
    paddingVertical: 0,
    fontFamily: fonts.actor400,
    fontSize: 18,
    color: colors.border,
    includeFontPadding: false,
  },

  // Više redova: tekst počinje gore, a ne u sredini, i ima mali razmak od obruba.
  inputRowMultiline: {
    alignItems: 'stretch',
  },
  inputMultiline: {
    paddingVertical: spacing.px8,
    textAlignVertical: 'top',
  },

  eye: {
    marginLeft: spacing.px8,
  },

  // Figma: Inter 16 / 22.4
  error: {
    fontFamily: fonts.inter400,
    fontSize: 16,
    lineHeight: 22.4,
    color: colors.error,
    includeFontPadding: false,
  },

  disabled: {
    opacity: 0.6,
  },
});

export default FormField;