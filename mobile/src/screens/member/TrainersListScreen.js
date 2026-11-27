import React, { useCallback, useMemo, useState } from 'react';
import { View, Text, FlatList, RefreshControl, ActivityIndicator, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import { EmptyState, ErrorMessage, FormField, ListItem } from '../../components';
import { theme } from '../../theme';
import { getTrainers } from '../../services/trainersService';
import { formatGrade } from '../../utils/grade';

const { colors, spacing, typography } = theme;
const MAX_FONT_SCALE = 1.3;

const LATIN = { č: 'c', ć: 'c', đ: 'dj', š: 's', ž: 'z' };
const normalize = (text) =>
  String(text ?? '')
    .toLowerCase()
    .replace(/[čćđšž]/g, (ch) => LATIN[ch])
    .trim();

const GradeBadge = ({ grade }) => (
  <View style={styles.grade}>
    {grade == null ? null : <Ionicons name="star" size={16} color={colors.primary} />}
    <Text
      style={[styles.gradeText, grade == null && styles.gradeEmpty]}
      maxFontSizeMultiplier={MAX_FONT_SCALE}
    >
      {formatGrade(grade)}
    </Text>
  </View>
);

export default function TrainersListScreen({ navigation }) {
  const [trainers, setTrainers] = useState(null);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setTrainers(await getTrainers());
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

  const query = normalize(search);
  const filtered = useMemo(() => {
    if (!trainers) return [];
    if (!query) return trainers;
    return trainers.filter((t) => {
      const nameFirst = normalize(`${t.name} ${t.surname}`);
      const surnameFirst = normalize(`${t.surname} ${t.name}`);
      return nameFirst.includes(query) || surnameFirst.includes(query);
    });
  }, [trainers, query]);

  if (loading && !trainers) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje trenera" />
      </View>
    );
  }

  if (!trainers) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={error} onRetry={() => { setLoading(true); load(); }} />
      </View>
    );
  }

  const noTrainers = trainers.length === 0;

  return (
    <View style={styles.screen}>
      <FlatList
        data={filtered}
        keyExtractor={(item) => String(item.trainerId)}
        renderItem={({ item }) => (
          <ListItem
            variant="person"
            icon="person-outline"
            title={`${item.name} ${item.surname}`}
            right={<GradeBadge grade={item.averageGrade} />}
            accessibilityLabel={`${item.name} ${item.surname}, prosječna ocjena ${formatGrade(item.averageGrade)}`}
            onPress={() => navigation.navigate('MemberTrainerProfile', { trainerId: item.trainerId })}
          />
        )}
        ListHeaderComponent={
          noTrainers ? null : (
            <View style={styles.header}>
              <FormField
                label="Pretraga"
                placeholder="Ime ili prezime trenera"
                value={search}
                onChangeText={setSearch}
                autoCorrect={false}
                returnKeyType="search"
              />
              {error ? <ErrorMessage error={error} inline /> : null}
            </View>
          )
        }
        ListEmptyComponent={
          noTrainers ? (
            <EmptyState icon="people-outline" title="Nema trenera" message="Trenutno nema dostupnih trenera." />
          ) : (
            <EmptyState
              icon="search-outline"
              title="Nema rezultata"
              message="Nijedan trener ne odgovara pretrazi."
            />
          )
        }
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
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
  grade: { flexDirection: 'row', alignItems: 'center', gap: spacing.px4 },
  gradeText: { ...typography.body, color: colors.primary },
  gradeEmpty: { ...typography.caption, color: colors.textSecondary },
});
