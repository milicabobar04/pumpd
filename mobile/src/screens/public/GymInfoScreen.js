import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  RefreshControl,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { ErrorMessage } from '../../components';
import { theme } from '../../theme';
import { getGymInfo } from '../../services/publicService';
import PublicHeader from './PublicHeader';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;
const EMPTY_VALUE = '—';

const formatPrice = (value) => {
  if (value === null || value === undefined || value === '') return EMPTY_VALUE;
  const n = Number(value);
  return Number.isFinite(n) ? `${n.toFixed(2).replace('.', ',')} KM` : EMPTY_VALUE;
};

const formatDate = (value) => {
  if (!value) return EMPTY_VALUE;
  const [year, month, day] = String(value).split('-');
  return year && month && day ? `${day.slice(0, 2)}.${month}.${year}.` : String(value);
};

const InfoCard = ({ icon, label, value }) => (
  <View style={styles.card} accessible accessibilityLabel={`${label}: ${value}`}>
    <View style={styles.cardHeader}>
      <Ionicons name={icon} size={20} color={colors.primary} />
      <Text style={styles.cardLabel} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        {label}
      </Text>
    </View>
    <Text style={styles.cardValue} maxFontSizeMultiplier={MAX_FONT_SCALE}>
      {value}
    </Text>
  </View>
);

export default function GymInfoScreen({ navigation }) {
  const [info, setInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [loadError, setLoadError] = useState(null);

  const loadInfo = useCallback(async () => {
    setLoadError(null);
    try {
      setInfo(await getGymInfo());
    } catch (e) {
      setLoadError(e);
    }
  }, []);

  useEffect(() => {
    loadInfo().finally(() => setLoading(false));
  }, [loadInfo]);

  const retry = async () => {
    setLoading(true);
    await loadInfo();
    setLoading(false);
  };

  const refresh = async () => {
    setRefreshing(true);
    await loadInfo();
    setRefreshing(false);
  };

  let body;
  if (loading && !info) {
    body = (
      <View style={styles.centered}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje informacija" />
      </View>
    );
  } else if (!info) {
    body = <ErrorMessage error={loadError} onRetry={retry} />;
  } else {
    body = (
      <>
        <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {info.name || EMPTY_VALUE}
        </Text>

        <ErrorMessage error={loadError} inline />

        <InfoCard icon="information-circle-outline" label="O teretani" value={info.description || EMPTY_VALUE} />
        <InfoCard icon="card-outline" label="Cijena članarine" value={formatPrice(info.membershipPrice)} />
        <InfoCard icon="time-outline" label="Radno vrijeme" value={info.workingHours || EMPTY_VALUE} />
        <InfoCard icon="calendar-outline" label="Datum osnivanja" value={formatDate(info.dateOfEstablishment)} />
      </>
    );
  }

  return (
    <View style={styles.screen}>
      <PublicHeader onBack={() => navigation.goBack()} />
      <ScrollView
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
      >
        {body}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  centered: {
    flexGrow: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: {
    flexGrow: 1,
    padding: spacing.px24,
    gap: spacing.px16,
  },
  title: {
    ...typography.screenTitle,
    color: colors.primary,
    textAlign: 'center',
    marginBottom: spacing.px8,
  },
  card: {
    width: '100%',
    gap: spacing.px8,
    padding: spacing.px16,
    backgroundColor: colors.surface,
    borderRadius: borderRadius.px10,
    borderLeftWidth: 3,
    borderLeftColor: colors.primary,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.px8,
  },
  cardLabel: {
    ...typography.caption,
    fontSize: 14,
    color: colors.textSecondary,
    textTransform: 'uppercase',
  },
  cardValue: {
    ...typography.body,
  },
});
