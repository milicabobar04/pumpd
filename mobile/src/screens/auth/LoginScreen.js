import React, { useRef, useState } from 'react';
import {
  Text,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useAuth } from '../../context/AuthContext';
import { login } from '../../services/authService';
import { Button, Card, FormField, ErrorMessage, Logo } from '../../components';
import { theme, cardSizes } from '../../theme';

const { colors, spacing, typography } = theme;
const MAX_FONT_SCALE = 1.3;

const INVALID_CREDENTIALS = 'Pogrešan email ili lozinka.';
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validate({ email, password }) {
  const errors = {};
  if (!email.trim()) errors.email = 'Unesite email.';
  else if (!EMAIL_PATTERN.test(email.trim())) errors.email = 'Email nije ispravan.';
  if (!password) errors.password = 'Unesite lozinku.';
  return errors;
}

export default function LoginScreen({ navigation }) {
  const { signIn } = useAuth();
  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const passwordRef = useRef(null);

  const setField = (key) => (value) => {
    setForm((f) => ({ ...f, [key]: value }));
    if (errors[key]) setErrors((e) => ({ ...e, [key]: undefined }));
    if (submitError) setSubmitError(null);
  };

  const submit = async () => {
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    setSubmitting(true);
    setSubmitError(null);
    try {
      const response = await login(form.email.trim(), form.password);

      await signIn(response);
    } catch (e) {
      const status = e?.response?.status;
      setSubmitError(status === 401 ? INVALID_CREDENTIALS : e);
      setForm((f) => ({ ...f, password: '' }));
      setSubmitting(false);
    }
  };

  return (
    <SafeAreaView style={styles.screen}>
      <KeyboardAvoidingView
        style={styles.screen}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <ScrollView
          contentContainerStyle={styles.content}
          keyboardShouldPersistTaps="handled"
        >
          <Logo height={56} />
          

          <Card variant="loginPanel" style={styles.panel}>
            <FormField
              label="Email"
              placeholder="ime@primjer.com"
              value={form.email}
              onChangeText={setField('email')}
              error={errors.email}
              editable={!submitting}
              keyboardType="email-address"
              autoCapitalize="none"
              autoCorrect={false}
              textContentType="emailAddress"
              autoComplete="email"
              returnKeyType="next"
              onSubmitEditing={() => passwordRef.current?.focus()}
              style={styles.field}
            />
            <FormField
              ref={passwordRef}
              label="Lozinka"
              value={form.password}
              onChangeText={setField('password')}
              error={errors.password}
              editable={!submitting}
              secureTextEntry
              autoCapitalize="none"
              autoCorrect={false}
              textContentType="password"
              autoComplete="current-password"
              returnKeyType="go"
              onSubmitEditing={submit}
              style={styles.field}
            />

            <ErrorMessage error={submitError} inline />

            <Button
              title="Prijavi se"
              variant="login"
              onPress={submit}
              loading={submitting}
            />
          </Card>

          <Button
            title="Informacije"
            variant="information"
            onPress={() => navigation.navigate('GymInfo')}
            disabled={submitting}
          />
          <Button
            title="Prijavite problem"
            variant="textLink"
            onPress={() => navigation.navigate('ReportIssue')}
            disabled={submitting}
          />
        </ScrollView>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    flexGrow: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: spacing.px24,
    gap: spacing.px16,
  },
  title: {
    ...typography.screenTitle,
  },
  panel: {
    width: '100%',
    maxWidth: cardSizes.loginPanel.width,
    alignItems: 'center',
    gap: spacing.px16,
  },
  field: {
    alignSelf: 'stretch',
  },
});
