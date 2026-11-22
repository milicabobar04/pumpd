import React, { useCallback, useState } from 'react';
import { View, Text, FlatList, Pressable, RefreshControl, ActivityIndicator, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import { EmptyState, ErrorMessage, ListItem } from '../../components';
import { theme } from '../../theme';
import { getPlans } from '../../services/trainingPlansService';

const { colors, spacing, typography } = theme;
const MAX_FONT_SCALE = 1.3;

const AddLink = ({ title, onPress }) => (
  <Pressable onPress={onPress} hitSlop={12} style={({ pressed }) => [styles.addLink, pressed && styles.pressed]}
    accessibilityRole="button" accessibilityLabel={title}>
    <Text style={styles.addLinkText} maxFontSizeMultiplier={MAX_FONT_SCALE}>{title}</Text>
    <Ionicons name="add" size={24} color={colors.primary} />
  </Pressable>
);

const planSubtitle = (plan) =>
  plan.description ? `${plan.durationMinutes} min · ${plan.description}` : `${plan.durationMinutes} min`;

export default function PlansScreen({ navigation }) {
  const [plans, setPlans] = useState(null); // null = još nije učitano
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setPlans(await getPlans());
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

  const openForm = () => navigation.navigate('PlanForm');

  if (loading && !plans) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje planova" />
      </View>
    );
  }

  if (!plans) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={error} onRetry={() => { setLoading(true); load(); }} />
      </View>
    );
  }

  return (
    <View style={styles.screen}>
      <FlatList
        data={plans}
        keyExtractor={(item) => String(item.planId)}
        renderItem={({ item }) => (
          <ListItem
            variant="exercise"
            icon="clipboard-outline"
            title={item.name}
            subtitle={planSubtitle(item)}
            onPress={() => navigation.navigate('PlanDetails', { planId: item.planId })}
          />
        )}
        ListHeaderComponent={
          <View style={styles.header}>
            <AddLink title="Kreiraj novi plan" onPress={openForm} />
            {error ? <ErrorMessage error={error} inline /> : null}
          </View>
        }
        ListEmptyComponent={
          <EmptyState icon="clipboard-outline" title="Nemate planova"
            message="Napravite prvi plan treninga." actionLabel="Kreiraj plan" onAction={openForm} />
        }
        contentContainerStyle={styles.content}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={refresh} tintColor={colors.primary}
            colors={[colors.primary]} progressBackgroundColor={colors.surface} />
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
  addLink: { flexDirection: 'row', alignItems: 'center', alignSelf: 'flex-end', gap: spacing.px8 },
  addLinkText: { ...typography.label, color: colors.primary },
  pressed: { opacity: 0.7 },
});
