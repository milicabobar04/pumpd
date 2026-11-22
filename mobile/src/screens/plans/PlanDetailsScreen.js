import React, { useCallback, useState } from 'react';
import { View, Text, ScrollView, ActivityIndicator, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Button, ConfirmDialog, EmptyState, ErrorMessage } from '../../components';
import { theme } from '../../theme';
import { getPlan, deletePlan } from '../../services/trainingPlansService';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;

const BLOCKED = {
  404: { icon: 'trash-outline', title: 'Plan više ne postoji', message: 'Ovaj plan je obrisan.' },
  403: { icon: 'lock-closed-outline', title: 'Nemate pristup', message: 'Ovaj plan pripada drugom korisniku.' },
};

export default function PlanDetailsScreen({ navigation, route }) {
  const { planId } = route.params;

  const [plan, setPlan] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [blocked, setBlocked] = useState(null); // 404 ili 403
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  const load = useCallback(async () => {
    setLoadError(null);
    try {
      setPlan(await getPlan(planId));
    } catch (e) {
      const status = e.response?.status;
      if (BLOCKED[status]) setBlocked(BLOCKED[status]);
      else setLoadError(e);
    } finally {
      setLoading(false);
    }
  }, [planId]);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const remove = async () => {
    setDeleting(true);
    setDeleteError(null);
    try {
      await deletePlan(planId);
      setConfirmOpen(false);
      navigation.goBack();
    } catch (e) {
      setConfirmOpen(false);
      if (e.response?.status === 404) navigation.goBack();
      else setDeleteError(e);
    } finally {
      setDeleting(false);
    }
  };

  if (loading && !plan) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje plana" />
      </View>
    );
  }

  if (blocked) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <EmptyState icon={blocked.icon} title={blocked.title} message={blocked.message} />
        <Button title="Nazad na listu" variant="action" onPress={() => navigation.goBack()} />
      </View>
    );
  }

  if (!plan) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={loadError} onRetry={() => { setLoading(true); load(); }} />
      </View>
    );
  }

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>{plan.name}</Text>
        <Text style={styles.meta} maxFontSizeMultiplier={MAX_FONT_SCALE}>Trajanje: {plan.durationMinutes} min</Text>
        {plan.description ? (
          <Text style={styles.description} maxFontSizeMultiplier={MAX_FONT_SCALE}>{plan.description}</Text>
        ) : null}

        <Text style={styles.sectionTitle} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          Vježbe ({plan.exercises.length})
        </Text>
        <View style={styles.card}>
          {plan.exercises.map((ex, i) => (
            <View key={ex.exerciseId ?? i} style={styles.exercise}
              accessible accessibilityLabel={`${i + 1}. ${ex.name}, ${ex.numSets} serija po ${ex.numRepetitions} ponavljanja`}>
              <Text style={styles.exerciseName} maxFontSizeMultiplier={MAX_FONT_SCALE}>{i + 1}. {ex.name}</Text>
              <Text style={styles.exerciseSets} maxFontSizeMultiplier={MAX_FONT_SCALE}>
                {ex.numSets} × {ex.numRepetitions}
              </Text>
            </View>
          ))}
        </View>

        <ErrorMessage error={deleteError} inline />

        <Button title="Uredi plan" variant="action" onPress={() => navigation.navigate('PlanForm', { planId })}
          style={styles.centerButton} />
        <Button title="Obriši plan" variant="danger" onPress={() => setConfirmOpen(true)} style={styles.centerButton} />
      </ScrollView>

      <ConfirmDialog
        visible={confirmOpen}
        title="Brisanje plana"
        message={`Da li ste sigurni da želite obrisati plan „${plan.name}“? Ova radnja se ne može poništiti.`}
        confirmLabel="Obriši"
        confirmVariant="danger"
        loading={deleting}
        onConfirm={remove}
        onCancel={() => setConfirmOpen(false)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  centered: { alignItems: 'center', justifyContent: 'center', gap: spacing.px16, padding: spacing.px24 },
  content: { flexGrow: 1, padding: spacing.px24, gap: spacing.px12 },
  title: { ...typography.screenTitle, color: colors.primary },
  meta: { ...typography.body },
  description: { ...typography.body, color: colors.textMuted },
  sectionTitle: { ...typography.sectionTitle, marginTop: spacing.px8 },
  card: { gap: spacing.px12, padding: spacing.px16, borderRadius: borderRadius.px10, backgroundColor: colors.surface },
  exercise: { flexDirection: 'row', justifyContent: 'space-between', gap: spacing.px12 },
  exerciseName: { ...typography.body, flex: 1 },
  exerciseSets: { ...typography.body, color: colors.primary },
  centerButton: { alignSelf: 'center' },
});
