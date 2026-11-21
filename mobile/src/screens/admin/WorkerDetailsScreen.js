import React, { useCallback, useEffect, useState } from 'react';
import { View, Text, ScrollView, ActivityIndicator, StyleSheet } from 'react-native';
import { Button, ConfirmDialog, EmptyState, ErrorMessage } from '../../components';
import { theme } from '../../theme';
import { getWorker, deleteWorker } from '../../services/workersAdminService';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;

const InfoRow = ({ label, value }) => (
  <View style={styles.row}>
    <Text style={styles.rowLabel} maxFontSizeMultiplier={MAX_FONT_SCALE}>{label}</Text>
    <Text style={styles.rowValue} maxFontSizeMultiplier={MAX_FONT_SCALE}>{value}</Text>
  </View>
);

export default function WorkerDetailsScreen({ navigation, route }) {
  const { workerId } = route.params;

  const [worker, setWorker] = useState(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState(null);
  const [notFound, setNotFound] = useState(false); // 404: radnik je obrisan (meko brisanje)
  const [confirmOpen, setConfirmOpen] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  const load = useCallback(async () => {
    setLoadError(null);
    try {
      setWorker(await getWorker(workerId));
    } catch (e) {
      // Obrisan radnik: ponovni pokušaj bi opet dao 404, pa ne nudimo "Pokušaj ponovo".
      if (e.response?.status === 404) setNotFound(true);
      else setLoadError(e);
    } finally {
      setLoading(false);
    }
  }, [workerId]);

  useEffect(() => {
    load();
  }, [load]);

  const remove = async () => {
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteWorker(workerId);
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

  if (loading) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje radnika" />
      </View>
    );
  }

  if (notFound) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <EmptyState
          icon="person-remove-outline"
          title="Radnik više ne postoji"
          message="Ovaj radnik je uklonjen."
        />
        <Button title="Nazad na listu" variant="action" onPress={() => navigation.goBack()} />
      </View>
    );
  }

  if (!worker) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={loadError} onRetry={() => { setLoading(true); load(); }} />
      </View>
    );
  }

  const fullName = `${worker.name} ${worker.surname}`;

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {fullName}
        </Text>
        <View style={styles.card}>
          <InfoRow label="Email" value={worker.email} />
        </View>
        <ErrorMessage error={deleteError} inline />
        <Button title="Ukloni radnika" variant="danger" onPress={() => setConfirmOpen(true)} style={styles.deleteButton} />
      </ScrollView>

      <ConfirmDialog
        visible={confirmOpen}
        title="Uklanjanje radnika"
        message={`Da li ste sigurni da želite ukloniti radnika ${fullName}? Ova radnja se ne može poništiti.`}
        confirmLabel="Ukloni"
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
  centered: { alignItems: 'center', justifyContent: 'center' },
  content: { flexGrow: 1, padding: spacing.px24, gap: spacing.px16 },
  title: { ...typography.screenTitle, color: colors.primary },
  card: { gap: spacing.px12, padding: spacing.px16, borderRadius: borderRadius.px10, backgroundColor: colors.surface },
  row: { gap: spacing.px2 },
  rowLabel: { ...typography.caption },
  rowValue: { ...typography.body },
  deleteButton: { alignSelf: 'center', marginTop: spacing.px8 },
});
