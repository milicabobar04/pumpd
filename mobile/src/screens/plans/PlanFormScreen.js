import React, { useEffect, useRef, useState } from 'react';
import { View, Text, ScrollView, Pressable, ActivityIndicator, KeyboardAvoidingView, Platform, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Button, ErrorMessage, FormField } from '../../components';
import { theme } from '../../theme';
import { getPlan, createPlan, updatePlan } from '../../services/trainingPlansService';
import { validateOptionalNumber, onlyErrors } from '../../utils/validation';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;
const MAX_NAME = 45;
const MAX_DESCRIPTION = 500;
const MAX_EXERCISES = 50;

function requiredInt(value, label, min, max) {
  if (!value.trim()) return `Unesite ${label.toLowerCase()}.`;
  return validateOptionalNumber(value, { label, min, max, integer: true });
}

function requiredName(value, label) {
  const v = value.trim();
  if (!v) return `${label} je obavezan.`;
  if (v.length > MAX_NAME) return `${label} može imati najviše ${MAX_NAME} znakova.`;
  return undefined;
}

function validate(form) {
  const errors = {
    name: requiredName(form.name, 'Naziv plana'),
    description: form.description.trim().length > MAX_DESCRIPTION
      ? `Opis može imati najviše ${MAX_DESCRIPTION} znakova.` : undefined,
    durationMinutes: requiredInt(form.durationMinutes, 'Trajanje', 1, 600),
    exercises: form.exercises.length === 0 ? 'Plan mora imati bar jednu vježbu.' : undefined,
  };
  form.exercises.forEach((ex) => {
    errors[`${ex.key}.name`] = requiredName(ex.name, 'Naziv vježbe');
    errors[`${ex.key}.numSets`] = requiredInt(ex.numSets, 'Broj serija', 1, 100);
    errors[`${ex.key}.numRepetitions`] = requiredInt(ex.numRepetitions, 'Broj ponavljanja', 1, 1000);
  });
  return onlyErrors(errors);
}

export default function PlanFormScreen({ navigation, route }) {
  const planId = route.params?.planId; // undefined = novi plan
  const nextKey = useRef(0);
  const newExercise = (data = {}) => ({
    key: nextKey.current++,
    exerciseId: null,
    name: '',
    numSets: '',
    numRepetitions: '',
    ...data,
  });

  const [form, setForm] = useState(() => ({
    name: '',
    description: '',
    durationMinutes: '',
    exercises: [newExercise()],
  }));
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(!!planId);
  const [loadError, setLoadError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState(null);

  const loadPlan = async () => {
    setLoadError(null);
    try {
      const plan = await getPlan(planId);
      setForm({
        name: plan.name,
        description: plan.description ?? '',
        durationMinutes: String(plan.durationMinutes),
        exercises: plan.exercises.map((ex) =>
          newExercise({
            exerciseId: ex.exerciseId,
            name: ex.name,
            numSets: String(ex.numSets),
            numRepetitions: String(ex.numRepetitions),
          }),
        ),
      });
    } catch (e) {
      setLoadError(e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (planId) loadPlan();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [planId]);

  const clearError = (key) => {
    if (errors[key]) setErrors((e) => ({ ...e, [key]: undefined }));
  };

  const setField = (key) => (value) => {
    setForm((f) => ({ ...f, [key]: value }));
    clearError(key);
  };

  const setExerciseField = (exKey, field) => (value) => {
    setForm((f) => ({
      ...f,
      exercises: f.exercises.map((ex) => (ex.key === exKey ? { ...ex, [field]: value } : ex)),
    }));
    clearError(`${exKey}.${field}`);
  };

  const addExercise = () => {
    setForm((f) => ({ ...f, exercises: [...f.exercises, newExercise()] }));
    clearError('exercises');
  };

  const removeExercise = (exKey) => {
    setForm((f) => ({ ...f, exercises: f.exercises.filter((ex) => ex.key !== exKey) }));
  };

  const save = async () => {
    const found = validate(form);
    setErrors(found);
    if (Object.keys(found).length > 0) return;

    const data = {
      name: form.name.trim(),
      description: form.description.trim() || null,
      durationMinutes: Number(form.durationMinutes),
      exercises: form.exercises.map((ex) => ({
        exerciseId: ex.exerciseId,
        name: ex.name.trim(),
        numSets: Number(ex.numSets),
        numRepetitions: Number(ex.numRepetitions),
      })),
    };

    setSaving(true);
    setSaveError(null);
    try {
      if (planId) {
        await updatePlan(planId, data);
        navigation.goBack();
      } else {
        const result = await createPlan(data);
        navigation.replace('PlanDetails', { planId: result.planId });
      }
    } catch (e) {
      setSaveError(e);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje plana" />
      </View>
    );
  }

  if (loadError) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={loadError} onRetry={() => { setLoading(true); loadPlan(); }} />
      </View>
    );
  }

  return (
    <KeyboardAvoidingView style={styles.screen} behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
        <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {planId ? 'Uređivanje plana' : 'Novi plan'}
        </Text>

        <FormField label="Naziv plana" value={form.name} onChangeText={setField('name')}
          error={errors.name} editable={!saving} maxLength={MAX_NAME} />
        <FormField label={`Opis (neobavezno, ${form.description.length}/${MAX_DESCRIPTION})`}
          value={form.description} onChangeText={setField('description')} error={errors.description}
          editable={!saving} multiline maxLength={MAX_DESCRIPTION} inputStyle={styles.multiline} />
        <FormField label="Trajanje (min)" value={form.durationMinutes} onChangeText={setField('durationMinutes')}
          error={errors.durationMinutes} editable={!saving} keyboardType="number-pad" maxLength={3} />

        <Text style={styles.sectionTitle} maxFontSizeMultiplier={MAX_FONT_SCALE}>Vježbe</Text>
        {errors.exercises ? <Text style={styles.error}>{errors.exercises}</Text> : null}

        {form.exercises.map((ex, i) => (
          <View key={ex.key} style={styles.exerciseCard}>
            <View style={styles.exerciseHeader}>
              <Text style={styles.exerciseTitle} maxFontSizeMultiplier={MAX_FONT_SCALE}>Vježba {i + 1}</Text>
              <Pressable
                onPress={() => removeExercise(ex.key)}
                disabled={saving || form.exercises.length === 1}
                hitSlop={12}
                accessibilityRole="button"
                accessibilityLabel={`Ukloni vježbu ${i + 1}`}
                accessibilityState={{ disabled: saving || form.exercises.length === 1 }}
                style={({ pressed }) => [pressed && styles.pressed, form.exercises.length === 1 && styles.disabled]}
              >
                <Ionicons name="trash-outline" size={22} color={colors.error} />
              </Pressable>
            </View>
            <FormField label="Naziv vježbe" value={ex.name} onChangeText={setExerciseField(ex.key, 'name')}
              error={errors[`${ex.key}.name`]} editable={!saving} maxLength={MAX_NAME} />
            <View style={styles.numbers}>
              <FormField label="Serije" value={ex.numSets} onChangeText={setExerciseField(ex.key, 'numSets')}
                error={errors[`${ex.key}.numSets`]} editable={!saving} keyboardType="number-pad" maxLength={3}
                style={styles.number} />
              <FormField label="Ponavljanja" value={ex.numRepetitions}
                onChangeText={setExerciseField(ex.key, 'numRepetitions')} error={errors[`${ex.key}.numRepetitions`]}
                editable={!saving} keyboardType="number-pad" maxLength={4} style={styles.number} />
            </View>
          </View>
        ))}

        {form.exercises.length < MAX_EXERCISES ? (
          <Button title="Dodaj vježbu" variant="add" onPress={addExercise} disabled={saving} style={styles.centerButton} />
        ) : null}

        <ErrorMessage error={saveError} inline />
        <Button title={planId ? 'Sačuvaj izmjene' : 'Kreiraj plan'} variant="action" onPress={save}
          loading={saving} style={styles.centerButton} />
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  centered: { alignItems: 'center', justifyContent: 'center' },
  content: { flexGrow: 1, padding: spacing.px24, gap: spacing.px16 },
  title: { ...typography.screenTitle, color: colors.primary },
  sectionTitle: { ...typography.sectionTitle },
  error: { ...typography.error },
  multiline: { minHeight: 96, textAlignVertical: 'top' },
  exerciseCard: { gap: spacing.px12, padding: spacing.px16, borderRadius: borderRadius.px10, backgroundColor: colors.surface },
  exerciseHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  exerciseTitle: { ...typography.body, color: colors.primary },
  numbers: { flexDirection: 'row', gap: spacing.px12 },
  number: { flex: 1 },
  centerButton: { alignSelf: 'center' },
  pressed: { opacity: 0.7 },
  disabled: { opacity: 0.3 },
});
