import React, { useRef, useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Button, FormField, ErrorMessage } from '../../components';
import { theme } from '../../theme';
import { changePassword } from '../../services/userService';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;

const MIN_LENGTH = 8;
const MAX_LENGTH = 72;

const EMPTY_FORM = { oldPassword: '', newPassword: '', confirmPassword: '' };
const SUCCESS_FALLBACK = 'Lozinka je uspješno promijenjena.';

const PASSWORD_INPUT = {
  secureTextEntry: true,
  autoCapitalize: 'none',
  autoCorrect: false,
};

function validate({ oldPassword, newPassword, confirmPassword }) {
  if (!oldPassword || !newPassword || !confirmPassword) {
    return 'Popunite sva tri polja.';
  }
  if (newPassword.length < MIN_LENGTH || newPassword.length > MAX_LENGTH) {
    return `Nova lozinka mora imati od ${MIN_LENGTH} do ${MAX_LENGTH} znakova.`;
  }
  if (newPassword !== confirmPassword) {
    return 'Nova lozinka i potvrda se ne poklapaju.';
  }
  if (newPassword === oldPassword) {
    return 'Nova lozinka mora biti različita od trenutne.';
  }
  return null;
}

export default function SettingsScreen() {
  const [form, setForm] = useState(EMPTY_FORM);
  const [error, setError] = useState(null);
  const [success, setSuccess] = useState(null);
  const [saving, setSaving] = useState(false);

  const newRef = useRef(null);
  const confirmRef = useRef(null);

  const setField = (key) => (value) => {
    setForm((f) => ({ ...f, [key]: value }));
    setError(null);
    setSuccess(null);
  };

  const save = async () => {
    const clientError = validate(form);
    if (clientError) {
      setError(clientError);
      setSuccess(null);
      return;
    }

    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const result = await changePassword(form.oldPassword, form.newPassword);
      setForm(EMPTY_FORM);
      setSuccess(result?.message || SUCCESS_FALLBACK);
    } catch (e) {
      setError(e);
    } finally {
      setSaving(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.screen}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <Text style={typography.screenTitle} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
          Promjena lozinke
        </Text>
        <Text style={styles.hint} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          Nova lozinka mora imati od {MIN_LENGTH} do {MAX_LENGTH} znakova.
        </Text>

        <FormField
          {...PASSWORD_INPUT}
          label="Trenutna lozinka"
          value={form.oldPassword}
          onChangeText={setField('oldPassword')}
          editable={!saving}
          textContentType="password"
          autoComplete="current-password"
          returnKeyType="next"
          onSubmitEditing={() => newRef.current?.focus()}
        />
        <FormField
          {...PASSWORD_INPUT}
          ref={newRef}
          label="Nova lozinka"
          value={form.newPassword}
          onChangeText={setField('newPassword')}
          editable={!saving}
          maxLength={MAX_LENGTH}
          textContentType="newPassword"
          autoComplete="new-password"
          returnKeyType="next"
          onSubmitEditing={() => confirmRef.current?.focus()}
        />
        <FormField
          {...PASSWORD_INPUT}
          ref={confirmRef}
          label="Potvrda nove lozinke"
          value={form.confirmPassword}
          onChangeText={setField('confirmPassword')}
          editable={!saving}
          maxLength={MAX_LENGTH}
          textContentType="newPassword"
          autoComplete="new-password"
          returnKeyType="done"
          onSubmitEditing={save}
        />

        <ErrorMessage error={error} inline />

        {success ? (
          <View style={styles.success} accessibilityRole="alert" accessibilityLiveRegion="polite">
            <Ionicons name="checkmark-circle-outline" size={20} color={colors.primary} />
            <Text style={styles.successText} maxFontSizeMultiplier={MAX_FONT_SCALE}>
              {success}
            </Text>
          </View>
        ) : null}

        <Button
          title="Sačuvaj lozinku"
          variant="action"
          onPress={save}
          loading={saving}
          style={styles.button}
        />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    flexGrow: 1,
    padding: spacing.px24,
    gap: spacing.px16,
  },
  hint: {
    ...typography.caption,
  },
  success: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.px8,
    padding: spacing.px12,
    backgroundColor: colors.surface,
    borderColor: colors.primary,
    borderWidth: 1,
    borderRadius: borderRadius.px8,
  },
  successText: {
    ...typography.body,
    flex: 1,
  },
  button: {
    alignSelf: 'center',
    marginTop: spacing.px8,
  },
});
