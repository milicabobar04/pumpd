import React, { useState } from 'react';
import { View, Text, ScrollView, KeyboardAvoidingView, Platform, StyleSheet } from 'react-native';
import { Button, ErrorMessage, FormField, StarRating } from '../../components';
import { theme } from '../../theme';
import { reviewTrainer } from '../../services/trainersService';

const { colors, spacing, typography } = theme;
const MAX_FONT_SCALE = 1.3;
const MAX_COMMENT = 500;

const GRADE_LABELS = { 1: 'Loše', 2: 'Osrednje', 3: 'Dobro', 4: 'Vrlo dobro', 5: 'Odlično' };

function validate(grade, comment) {
  const errors = {};
  if (!Number.isInteger(grade) || grade < 1 || grade > 5) errors.grade = 'Izaberite ocjenu od 1 do 5.';
  if (comment.length > MAX_COMMENT) errors.comment = `Komentar može imati najviše ${MAX_COMMENT} znakova.`;
  return errors;
}

export default function TrainerReviewScreen({ navigation, route }) {
  const { trainerId, trainerName } = route.params;
  const [grade, setGrade] = useState(0);
  const [comment, setComment] = useState('');
  const [errors, setErrors] = useState({});
  const [sending, setSending] = useState(false);
  const [sendError, setSendError] = useState(null);

  const pickGrade = (value) => {
    setGrade(value);
    if (errors.grade) setErrors((e) => ({ ...e, grade: undefined }));
  };

  const changeComment = (value) => {
    setComment(value);
    if (errors.comment) setErrors((e) => ({ ...e, comment: undefined }));
  };

  const send = async () => {
    const trimmed = comment.trim();
    const found = validate(grade, trimmed);
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    setSending(true);
    setSendError(null);
    try {
      const result = await reviewTrainer(trainerId, { grade, comment: trimmed });
      navigation.popTo('MemberTrainerProfile', {
        trainerId,
        reviewMessage: result?.message || 'Recenzija je sačuvana.',
      });
    } catch (e) {
      setSendError(e);
      setSending(false);
    }
  };

  return (
    <KeyboardAvoidingView style={styles.screen} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
          Recenzija
        </Text>
        {trainerName ? (
          <Text style={styles.subtitle} maxFontSizeMultiplier={MAX_FONT_SCALE}>
            Ocijenite trenera {trainerName}. Ako ste ga već ocijenili, nova recenzija zamjenjuje staru.
          </Text>
        ) : null}

        <View style={styles.gradeBlock}>
          <Text style={styles.label} maxFontSizeMultiplier={MAX_FONT_SCALE}>Ocjena</Text>
          <StarRating value={grade} onChange={pickGrade} size={40} disabled={sending} />
          <Text style={styles.gradeHint} maxFontSizeMultiplier={MAX_FONT_SCALE}>
            {grade ? `${grade}/5 · ${GRADE_LABELS[grade]}` : 'Dodirnite zvjezdicu'}
          </Text>
          {errors.grade ? (
            <Text style={styles.error} maxFontSizeMultiplier={MAX_FONT_SCALE}>{errors.grade}</Text>
          ) : null}
        </View>

        <View>
          <FormField
            label="Komentar (opciono)"
            placeholder="Kako vam je bilo sa ovim trenerom?"
            value={comment}
            onChangeText={changeComment}
            error={errors.comment}
            editable={!sending}
            multiline
            maxLength={MAX_COMMENT}
            textAlignVertical="top"
            inputStyle={styles.commentInput}
          />
          <Text style={styles.counter} maxFontSizeMultiplier={MAX_FONT_SCALE}>
            {comment.length}/{MAX_COMMENT}
          </Text>
        </View>

        <ErrorMessage error={sendError} inline />

        <Button
          title="Pošalji recenziju"
          variant="action"
          onPress={send}
          loading={sending}
          style={styles.sendButton}
        />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  content: { flexGrow: 1, padding: spacing.px24, gap: spacing.px16 },
  title: { ...typography.screenTitle, color: colors.primary },
  subtitle: { ...typography.body, color: colors.textSecondary },
  gradeBlock: { gap: spacing.px8 },
  label: { ...typography.label, color: colors.primary },
  gradeHint: { ...typography.caption, color: colors.textSecondary },
  error: { ...typography.error },
  commentInput: { minHeight: 120 },
  counter: { ...typography.caption, alignSelf: 'flex-end', marginTop: spacing.px4 },
  sendButton: { alignSelf: 'center', marginTop: spacing.px8 },
});
