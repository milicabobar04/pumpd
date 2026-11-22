import React, { useCallback, useState } from 'react';
import { View, Text, ScrollView, RefreshControl, ActivityIndicator, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { ErrorMessage } from '../../components';
import { theme } from '../../theme';
import { getOccupancy } from '../../services/attendanceService';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;
const REFRESH_MS = 30000;

function level(percent) {
  if (percent < 50) return { label: 'Slobodno', color: colors.primary };
  if (percent < 80) return { label: 'Umjereno', color: colors.textPrimary };
  return { label: 'Gužva', color: colors.error };
}

const timeNow = () => new Date().toTimeString().slice(0, 5);

export default function GymOccupancyScreen() {
  const [data, setData] = useState(null);
  const [updatedAt, setUpdatedAt] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setData(await getOccupancy());
      setUpdatedAt(timeNow());
    } catch (e) {
      setError(e);
    } finally {
      setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load();
      const timer = setInterval(load, REFRESH_MS);
      return () => clearInterval(timer);
    }, [load]),
  );

  const refresh = async () => {
    setRefreshing(true);
    await load();
    setRefreshing(false);
  };

  if (loading) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje zauzetosti" />
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

  const { currentMembers, capacity } = data;
  const percent = capacity ? Math.min(100, Math.round((currentMembers / capacity) * 100)) : null;
  const lvl = percent === null ? null : level(percent);

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={styles.content}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={refresh} tintColor={colors.primary}
          colors={[colors.primary]} progressBackgroundColor={colors.surface} />
      }
    >
      <Text style={styles.number} maxFontSizeMultiplier={MAX_FONT_SCALE}
        accessibilityLabel={`U teretani je ${currentMembers}${capacity ? ` od ${capacity}` : ''} članova`}>
        {currentMembers}{capacity ? <Text style={styles.capacity}> / {capacity}</Text> : null}
      </Text>
      <Text style={styles.caption} maxFontSizeMultiplier={MAX_FONT_SCALE}>članova je trenutno u teretani</Text>

      {lvl ? (
        <>
          <View style={styles.bar} accessibilityElementsHidden importantForAccessibility="no-hide-descendants">
            <View style={[styles.fill, { width: `${percent}%`, backgroundColor: lvl.color }]} />
          </View>
          <Text style={[styles.level, { color: lvl.color }]} maxFontSizeMultiplier={MAX_FONT_SCALE}>
            {lvl.label} ({percent} %)
          </Text>
        </>
      ) : null}

      <Text style={styles.updated} maxFontSizeMultiplier={MAX_FONT_SCALE}>Ažurirano u {updatedAt}</Text>
      {error ? <ErrorMessage error={error} inline /> : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  centered: { alignItems: 'center', justifyContent: 'center' },
  content: { flexGrow: 1, padding: spacing.px24, gap: spacing.px12, alignItems: 'center', justifyContent: 'center' },
  number: { ...typography.screenTitle, fontSize: 64, color: colors.primary },
  capacity: { fontSize: 32, color: colors.textMuted },
  caption: { ...typography.body },
  bar: { alignSelf: 'stretch', height: 12, borderRadius: borderRadius.px8, backgroundColor: colors.surface, overflow: 'hidden' },
  fill: { height: '100%' },
  level: { ...typography.sectionTitle },
  updated: { ...typography.caption },
});