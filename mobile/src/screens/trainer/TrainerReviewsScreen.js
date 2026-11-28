import React, { useCallback, useState } from 'react';
import { View, Text, FlatList, RefreshControl, ActivityIndicator, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { EmptyState, ErrorMessage, ReviewCard, StarRating } from '../../components';
import { theme } from '../../theme';
import { getMyReviews } from '../../services/trainerService';
import { formatGrade } from '../../utils/grade';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;

// Trener: prosječna ocjena i sve recenzije, od najnovije (SU-07)
export default function TrainerReviewsScreen() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setData(await getMyReviews());
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
    await load();
    setRefreshing(false);
  };

  if (loading && !data) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje recenzija" />
      </View>
    );
  }

  if (!data) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={error} onRetry={() => { setLoading(true); load(); }} />
      </View>
    );
  }

  const noGrade = data.averageGrade == null;

  const header = (
    <View style={styles.header}>
      <View style={styles.card}>
        <Text style={styles.rowLabel} maxFontSizeMultiplier={MAX_FONT_SCALE}>Prosječna ocjena</Text>
        <View style={styles.gradeRow}>
          {noGrade ? null : <StarRating value={data.averageGrade} size={24} />}
          <Text
            style={noGrade ? styles.gradeEmpty : styles.gradeText}
            maxFontSizeMultiplier={MAX_FONT_SCALE}
            accessibilityLabel={noGrade ? 'Još nemate ocjena' : `Prosječna ocjena ${formatGrade(data.averageGrade)}`}
          >
            {noGrade ? 'Još nemate ocjena' : formatGrade(data.averageGrade)}
          </Text>
        </View>
      </View>

      {error ? <ErrorMessage error={error} inline /> : null}

      <Text style={styles.sectionTitle} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
        Recenzije ({data.reviews.length})
      </Text>
    </View>
  );

  return (
    <View style={styles.screen}>
      <FlatList
        data={data.reviews}
        keyExtractor={(item) => String(item.reviewId)}
        renderItem={({ item }) => <ReviewCard review={item} />}
        ListHeaderComponent={header}
        ListEmptyComponent={
          <EmptyState
            icon="chatbubble-ellipses-outline"
            title="Nema recenzija"
            message="Članovi vas još nisu ocijenili. Povucite nadolje da osvježite listu."
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
  content: { flexGrow: 1, padding: spacing.px24, gap: spacing.px12 },
  header: { gap: spacing.px16, marginBottom: spacing.px4 },
  card: { gap: spacing.px8, padding: spacing.px16, borderRadius: borderRadius.px10, backgroundColor: colors.surface },
  rowLabel: { ...typography.caption, color: colors.textSecondary },
  gradeRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.px8 },
  gradeText: { ...typography.sectionTitle, color: colors.primary },
  gradeEmpty: { ...typography.body, color: colors.textSecondary },
  sectionTitle: { ...typography.sectionTitle },
});
