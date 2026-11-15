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
import { reportIssue } from '../../services/publicService';
import PublicHeader from './PublicHeader';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;

const STATUS_LABELS = {
  UNPROCESSED: 'Neobrađen',
  IN_PROGRESS: 'U obradi',
  PROCESSED: 'Obrađen',
};

function validate(form) {
  const errors = {};
  if (!form.title.trim()) errors.title = 'Unesite naslov problema.';
  if (!form.description.trim()) errors.description = 'Unesite opis problema.';
  return errors;
}

const ResultRow = ({ label, value }) => (
  <View style={styles.resultRow}>
    <Text style={styles.resultLabel} maxFontSizeMultiplier={MAX_FONT_SCALE}>
      {label}
    </Text>
    <Text style={styles.resultValue} maxFontSizeMultiplier={MAX_FONT_SCALE}>
      {value}
    </Text>
  </View>
);

const IssueSent = ({ result, onBack }) => (
  <View style={styles.result} accessibilityRole="alert" accessibilityLiveRegion="polite">
    <Ionicons name="checkmark-circle-outline" size={72} color={colors.primary} />
    <Text style={styles.resultTitle} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
      Prijava je poslana
    </Text>
    {result.message ? (
      <Text style={styles.resultMessage} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        {result.message}
      </Text>
    ) : null}

    <View style={styles.resultCard}>
      <ResultRow label="Broj prijave" value={`#${result.issueId}`} />
      <ResultRow label="Status" value={STATUS_LABELS[result.status] ?? result.status} />
    </View>

    <Button title="Nazad" variant="action" onPress={onBack} />
  </View>
);

export default function ReportIssueScreen({ navigation, route }) {
  const embedded = route?.params?.embedded === true;

  const [form, setForm] = useState({ title: '', description: '' });
  const [errors, setErrors] = useState({});
  const [sendError, setSendError] = useState(null);
  const [sending, setSending] = useState(false);
  const [result, setResult] = useState(null);

  const descriptionRef = useRef(null);

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
      setResult(
        await reportIssue({
          title: form.title.trim(),
          description: form.description.trim(),
        }),
      );
    } catch (e) {
      setSendError(e);
    } finally {
      setSending(false);
    }
  };

  const goBack = () => navigation.goBack();

  return (
    <KeyboardAvoidingView
      style={styles.screen}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      {embedded ? null : <PublicHeader onBack={goBack} />}

      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        {result ? (
          <IssueSent result={result} onBack={goBack} />
        ) : (
          <>
            <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
              Prijavite problem
            </Text>
            <Text style={styles.subtitle} maxFontSizeMultiplier={MAX_FONT_SCALE}>
              Opišite problem koji ste primijetili, a administrator će ga pregledati.
            </Text>

            <FormField
              label="Naslov"
              placeholder="npr. Pokvarena traka za trčanje"
              value={form.title}
              onChangeText={setField('title')}
              error={errors.title}
              editable={!sending}
              returnKeyType="next"
              onSubmitEditing={() => descriptionRef.current?.focus()}
            />
            <FormField
              ref={descriptionRef}
              label="Opis problema"
              placeholder="Šta se desilo i gdje?"
              value={form.description}
              onChangeText={setField('description')}
              error={errors.description}
              editable={!sending}
              multiline
              textAlignVertical="top"
              inputStyle={styles.descriptionInput}
            />

            <ErrorMessage error={sendError} inline />

            <Button
              title="Pošalji prijavu"
              variant="action"
              onPress={send}
              loading={sending}
              style={styles.submit}
            />
          </>
        )}
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
  title: {
    ...typography.screenTitle,
    color: colors.primary,
  },
  subtitle: {
    ...typography.body,
    color: colors.textSecondary,
    marginBottom: spacing.px8,
  },
  descriptionInput: {
    minHeight: 140,
  },
  submit: {
    alignSelf: 'center',
    marginTop: spacing.px8,
  },

  result: {
    flexGrow: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: spacing.px16,
  },
  resultTitle: {
    ...typography.screenTitle,
    textAlign: 'center',
  },
  resultMessage: {
    ...typography.body,
    color: colors.textSecondary,
    textAlign: 'center',
  },
  resultCard: {
    width: '100%',
    gap: spacing.px12,
    padding: spacing.px16,
    backgroundColor: colors.surface,
    borderRadius: borderRadius.px10,
    borderColor: colors.primary,
    borderWidth: 1,
  },
  resultRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: spacing.px12,
  },
  resultLabel: {
    ...typography.body,
    color: colors.textSecondary,
  },
  resultValue: {
    ...typography.body,
    color: colors.primary,
    flexShrink: 1,
    textAlign: 'right',
  },
});
