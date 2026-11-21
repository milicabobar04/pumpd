import React, { useState } from 'react';
import { Text, ScrollView, KeyboardAvoidingView, Platform, StyleSheet } from 'react-native';
import { AccountCreated, Button, ErrorMessage, FormField, getErrorMessage } from '../../components';
import { theme } from '../../theme';
import { addMember } from '../../services/membersService';
import {
  validateName,
  validateEmail,
  validateOptionalNumber,
  toNumberOrNull,
  onlyErrors,
} from '../../utils/validation';

const { colors, spacing, typography } = theme;
const MAX_FONT_SCALE = 1.3;

const EMPTY_FORM = { name: '', surname: '', email: '', birthYear: '', weight: '', height: '' };

function validate(form) {
  const currentYear = new Date().getFullYear();
  return onlyErrors({
    name: validateName(form.name, 'Ime'),
    surname: validateName(form.surname, 'Prezime'),
    email: validateEmail(form.email),
    birthYear: validateOptionalNumber(form.birthYear, { label: 'Godina rođenja', min: 1900, max: currentYear, integer: true }),
    weight: validateOptionalNumber(form.weight, { label: 'Težina', min: 0, max: 500, minExclusive: true }),
    height: validateOptionalNumber(form.height, { label: 'Visina', min: 0, max: 300, minExclusive: true }),
  });
}

export default function AddMemberScreen({ navigation }) {
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
      const result = await addMember({
        name: form.name.trim(),
        surname: form.surname.trim(),
        email: form.email.trim(),
        birthYear: toNumberOrNull(form.birthYear),
        weight: toNumberOrNull(form.weight),
        height: toNumberOrNull(form.height),
      });
      setCreated(result);
    } catch (e) {
      if (e.response?.status === 409) {
        setErrors((prev) => ({ ...prev, email: getErrorMessage(e) }));
      } else {
        setSendError(e);
      }
    } finally {
      setSending(false);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.screen} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {created ? (
          <AccountCreated
            title="Član je dodan"
            fullName={`${form.name.trim()} ${form.surname.trim()}`}
            email={form.email.trim()}
            temporaryPassword={created.temporaryPassword}
            onDone={() => navigation.goBack()}
          />
        ) : (
          <>
            <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
              Novi član
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
              label="Godina rođenja (neobavezno)"
              value={form.birthYear}
              onChangeText={setField('birthYear')}
              error={errors.birthYear}
              editable={!sending}
              keyboardType="number-pad"
              maxLength={4}
            />
            <FormField
              label="Težina u kg (neobavezno)"
              value={form.weight}
              onChangeText={setField('weight')}
              error={errors.weight}
              editable={!sending}
              keyboardType="decimal-pad"
            />
            <FormField
              label="Visina u cm (neobavezno)"
              value={form.height}
              onChangeText={setField('height')}
              error={errors.height}
              editable={!sending}
              keyboardType="decimal-pad"
            />

            <ErrorMessage error={sendError} inline />

            <Button title="Dodaj člana" variant="action" onPress={send} loading={sending} style={styles.submit} />
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