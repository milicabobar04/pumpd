import React, { useCallback, useState } from 'react';
import { View, Text, FlatList, RefreshControl, ActivityIndicator, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import { Button, EmptyState, ErrorMessage } from '../../components';
import { theme } from '../../theme';
import { getAdminIssues, updateIssueStatus, ISSUE_STATUSES } from '../../services/issuesService';
import { formatDate } from '../../utils/date';

const { colors, spacing, borderRadius, typography, fonts } = theme;
const MAX_FONT_SCALE = 1.3;

const STATUS_LABELS = {
  UNPROCESSED: 'Neobrađen',
  IN_PROGRESS: 'U obradi',
  PROCESSED: 'Obrađen',
};

const STATUS_COLORS = {
  UNPROCESSED: colors.error,
  IN_PROGRESS: colors.accentPurpleLight,
  PROCESSED: colors.primary,
};

const statusLabel = (status) => STATUS_LABELS[status] || status || '–';

// Poruka sa servera, osim generičke "string" koju vraća Prism mock (primjer iz šeme).
const serverMessage = (message) =>
  typeof message === 'string' && message.trim() && message !== 'string' ? message : null;

const IssueCard = ({ issue, updating, onChangeStatus }) => (
  <View style={styles.card}>
    <View style={styles.cardHeader}>
      <Ionicons name="build-outline" size={22} color={colors.iconMuted} />
      <Text style={styles.cardTitle} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        {issue.title}
      </Text>
    </View>

    <View style={styles.meta}>
      <Text style={styles.date} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        Prijavljeno: {formatDate(issue.reportDate)}
      </Text>
      <View style={[styles.badge, { borderColor: STATUS_COLORS[issue.status] || colors.border }]}>
        <Text
          style={[styles.badgeText, { color: STATUS_COLORS[issue.status] || colors.border }]}
          maxFontSizeMultiplier={MAX_FONT_SCALE}
        >
          {statusLabel(issue.status)}
        </Text>
      </View>
    </View>

    {issue.description ? (
      <Text style={styles.description} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        {issue.description}
      </Text>
    ) : null}

    <Text style={styles.changeLabel} maxFontSizeMultiplier={MAX_FONT_SCALE}>
      Promijeni status:
    </Text>
    <View style={styles.chips}>
      {updating ? (
        <ActivityIndicator color={colors.primary} accessibilityLabel="Promjena statusa" />
      ) : (
        ISSUE_STATUSES.map((status) => {
          const selected = issue.status === status;
          return (
            <Button
              key={status}
              title={STATUS_LABELS[status]}
              variant={selected ? 'chipSelected' : 'chip'}
              onPress={() => (selected ? undefined : onChangeStatus(issue, status))}
              style={styles.chip}
            />
          );
        })
      )}
    </View>
  </View>
);

export default function IssuesScreen() {
  const [issues, setIssues] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const [updatingId, setUpdatingId] = useState(null);
  const [success, setSuccess] = useState(null);
  const [updateError, setUpdateError] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setIssues(await getAdminIssues());
    } catch (e) {
      setError(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
    }, [load]),
  );

  const refresh = async () => {
    setRefreshing(true);
    setSuccess(null);
    setUpdateError(null);
    await load();
    setRefreshing(false);
  };

  const changeStatus = async (issue, status) => {
    if (updatingId !== null) return;
    setUpdatingId(issue.issueId);
    setSuccess(null);
    setUpdateError(null);
    try {
      const result = await updateIssueStatus(issue.issueId, { status });
      // Prikazuje se status koji je administrator izabrao. Prism mock uvijek vraća prvu
      // enum vrijednost (UNPROCESSED), a pravi backend vraća upravo poslani status.
      setIssues((list) =>
        (list || []).map((i) => (i.issueId === issue.issueId ? { ...i, status } : i)),
      );
      setSuccess(
        serverMessage(result?.message) ||
          `Status problema "${issue.title}" je promijenjen u "${statusLabel(status)}".`,
      );
    } catch (e) {
      setUpdateError(e);
    } finally {
      setUpdatingId(null);
    }
  };

  if (loading && !issues) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje prijavljenih problema" />
      </View>
    );
  }

  if (!issues) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={error} onRetry={() => { setLoading(true); load(); }} />
      </View>
    );
  }

  return (
    <View style={styles.screen}>
      <FlatList
        data={issues}
        keyExtractor={(item) => String(item.issueId)}
        renderItem={({ item }) => (
          <IssueCard issue={item} updating={updatingId === item.issueId} onChangeStatus={changeStatus} />
        )}
        ListHeaderComponent={
          error || updateError || success ? (
            <View style={styles.header}>
              {error ? <ErrorMessage error={error} inline /> : null}
              {updateError ? <ErrorMessage error={updateError} inline /> : null}
              {success ? (
                <View style={styles.success} accessibilityRole="alert" accessibilityLiveRegion="polite">
                  <Ionicons name="checkmark-circle-outline" size={20} color={colors.primary} />
                  <Text style={styles.successText} maxFontSizeMultiplier={MAX_FONT_SCALE}>
                    {success}
                  </Text>
                </View>
              ) : null}
            </View>
          ) : null
        }
        ListEmptyComponent={
          <EmptyState
            icon="checkmark-done-outline"
            title="Nema prijavljenih problema"
            message="Trenutno nema problema za obradu."
          />
        }
        contentContainerStyle={styles.content}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={refresh}
            tintColor={colors.primary}
            colors={[colors.primary]}
            progressBackgroundColor={colors.surface}
          />
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  centered: { alignItems: 'center', justifyContent: 'center' },
  content: { flexGrow: 1, padding: spacing.px16, gap: spacing.px12 },
  header: { gap: spacing.px12, marginBottom: spacing.px4 },

  card: {
    backgroundColor: colors.surface,
    borderColor: colors.border,
    borderWidth: 1,
    borderRadius: borderRadius.px10,
    padding: spacing.px16,
    gap: spacing.px10,
  },
  cardHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.px8 },
  cardTitle: {
    flex: 1,
    fontFamily: fonts.actor400,
    fontSize: 18,
    color: colors.textLight,
  },
  meta: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: spacing.px8,
  },
  date: { ...typography.caption, color: colors.textSecondary },
  badge: {
    borderWidth: 1,
    borderRadius: borderRadius.px8,
    paddingHorizontal: spacing.px8,
    paddingVertical: spacing.px2,
  },
  badgeText: { fontFamily: fonts.inter500, fontSize: 13 },
  description: { ...typography.body, color: colors.textLight },
  changeLabel: { ...typography.caption, color: colors.textSecondary },
  chips: { flexDirection: 'row', flexWrap: 'wrap', alignItems: 'center', gap: spacing.px8, minHeight: 24 },
  chip: { width: 'auto', maxWidth: undefined },

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
  successText: { ...typography.body, flex: 1 },
});
