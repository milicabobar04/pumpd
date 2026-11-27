import React, { useState } from 'react';
import { Text, ScrollView, KeyboardAvoidingView, Platform, StyleSheet } from 'react-native';
import { AccountCreated, Button, ErrorMessage, FormField, getErrorMessage } from '../../components';
import { theme } from '../../theme';
import { addTrainer } from '../../services/trainersAdminService';
import { validateName, validateEmail, onlyErrors } from '../../utils/validation';

const { colors, spacing, typography } = theme;
const MAX_FONT_SCALE = 1.3;
const MAX_BIOGRAPHY = 500;

const EMPTY_FORM = { name: '', surname: '', email: '', biography: '' };

function validate(form) {
  return onlyErrors({
    name: validateName(form.name, 'Ime'),
    surname: validateName(form.surname, 'Prezime'),
    email: validateEmail(form.email),
    biography:
      form.biography.trim().length > MAX_BIOGRAPHY
        ? `Biografija može imati najviše ${MAX_BIOGRAPHY} znakova.`
        : undefined,
  });
}

export default function AddTrainerScreen({ navigation }) {
  const [form, setForm] = useState(EMPTY_FORM);
  const [errors, setErrors] = useState({});
  const [sendError, setSendError] = useState(null);
  const [sending, setSending] = useState(false);
  const [created, setCreated] = useState(null);

  const setField = (key) => (value) => {
    setForm((f) => ({ ...f, [key]: value }));
    if (errors[key]) setErrors((e) => ({ ...e, [key]: undefined }));
  };

  const send = async () => {
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    setSending(true);
    setSendError(null);
    try {
      setCreated(
        await addTrainer({
          name: form.name.trim(),
          surname: form.surname.trim(),
          email: form.email.trim(),
          biography: form.biography.trim() || null,
        }),
      );
    } catch (e) {
      if (e.response?.status === 409) setErrors((prev) => ({ ...prev, email: getErrorMessage(e) }));
      else setSendError(e);
    } finally {
      setSending(false);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.screen} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {created ? (
          <AccountCreated
            title="Trener je dodan"
            fullName={`${form.name.trim()} ${form.surname.trim()}`}
            email={form.email.trim()}
            temporaryPassword={created.temporaryPassword}
            onDone={() => navigation.goBack()}
          />
        ) : (
          <>
            <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
              Novi trener
            </Text>

            <FormField label="Ime" value={form.name} onChangeText={setField('name')} error={errors.name} editable={!sending} />
            <FormField label="Prezime" value={form.surname} onChangeText={setField('surname')} error={errors.surname} editable={!sending} />
            <FormField
              label="Email"
              value={form.email}
              onChangeText={setField('email')}
              error={errors.email}
              editable={!sending}
              keyboardType="email-address"
              autoCapitalize="none"
              autoCorrect={false}
            />
            <FormField
              label={`Biografija (neobavezno, ${form.biography.length}/${MAX_BIOGRAPHY})`}
              value={form.biography}
              onChangeText={setField('biography')}
              error={errors.biography}
              editable={!sending}
              multiline
              textAlignVertical="top"
              inputStyle={{ minHeight: 120 }}
            />

            <ErrorMessage error={sendError} inline />

            <Button title="Dodaj trenera" variant="action" onPress={send} loading={sending} style={styles.submit} />
          </>
        )}
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  content: { flexGrow: 1, padding: spacing.px24, gap: spacing.px16 },
  title: { ...typography.screenTitle, color: colors.primary },
  submit: { alignSelf: 'center', marginTop: spacing.px8 },
});