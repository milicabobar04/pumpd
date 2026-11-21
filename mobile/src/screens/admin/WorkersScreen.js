import React, { useCallback, useState } from 'react';
import { View, FlatList, RefreshControl, ActivityIndicator, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Button, EmptyState, ErrorMessage, ListItem } from '../../components';
import { theme } from '../../theme';
import { getWorkers } from '../../services/workersAdminService';

const { colors, spacing } = theme;

export default function WorkersScreen({ navigation }) {
  const [workers, setWorkers] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setWorkers(await getWorkers());
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

  if (loading && !workers) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje radnika" />
      </View>
    );
  }

  if (!workers) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={error} onRetry={() => { setLoading(true); load(); }} />
      </View>
    );
  }

  return (
    <View style={styles.screen}>
      <FlatList
        data={workers}
        keyExtractor={(item) => String(item.workerId)}
        renderItem={({ item }) => (
          <ListItem
            variant="person"
            icon="person-outline"
            title={`${item.name} ${item.surname}`}
            subtitle={item.email}
            onPress={() => navigation.navigate('AdminWorkerDetails', { workerId: item.workerId })}
          />
        )}
        ListHeaderComponent={
          <View style={styles.header}>
            <Button
              title="Dodaj radnika"
              variant="add"
              onPress={() => navigation.navigate('AdminAddWorker')}
              style={styles.addButton}
            />
            {error ? <ErrorMessage error={error} inline /> : null}
          </View>
        }
        ListEmptyComponent={
          <EmptyState icon="people-outline" title="Nema radnika" message="Dodajte prvog radnika." />
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
