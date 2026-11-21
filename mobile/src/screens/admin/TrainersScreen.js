import React, { useCallback, useState } from 'react';
import { View, FlatList, RefreshControl, ActivityIndicator, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Button, EmptyState, ErrorMessage, ListItem } from '../../components';
import { theme } from '../../theme';
import { getTrainers } from '../../services/trainersAdminService';

const { colors, spacing } = theme;

export default function TrainersScreen({ navigation }) {
  const [trainers, setTrainers] = useState(null);
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

  return (
    <View style={styles.screen}>
      <FlatList
        data={trainers}
        keyExtractor={(item) => String(item.trainerId)}
        renderItem={({ item }) => (
          <ListItem
            variant="person"
            icon="person-outline"
            title={`${item.name} ${item.surname}`}
            subtitle={item.email}
            onPress={() => navigation.navigate('AdminTrainerDetails', { trainerId: item.trainerId })}
          />
        )}
        ListHeaderComponent={
          <View style={styles.header}>
            <Button
              title="Dodaj trenera"
              variant="add"
              onPress={() => navigation.navigate('AdminAddTrainer')}
              style={styles.addButton}
            />
            {error ? <ErrorMessage error={error} inline /> : null}
          </View>
        }
        ListEmptyComponent={
          <EmptyState icon="people-outline" title="Nema trenera" message="Dodajte prvog trenera." />
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
  addButton: { alignSelf: 'center' },
});