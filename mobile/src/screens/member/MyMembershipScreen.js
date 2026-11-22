import React, { useCallback, useState } from 'react';
import { View, Text, Image, ScrollView, RefreshControl, ActivityIndicator, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { EmptyState, ErrorMessage } from '../../components';
import { theme } from '../../theme';
import { getMyMembership } from '../../services/membershipService';
import { formatDate } from '../../utils/date';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;
const QR_SIZE = 240;

export default function MyMembershipScreen() {
  const [membership, setMembership] = useState(null);
  const [missing, setMissing] = useState(false); // 404: nema članarinu
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const load = useCallback(async () => {
    setError(null);
    try {
      setMembership(await getMyMembership());
      setMissing(false);
    } catch (e) {
      if (e.response?.status === 404) setMissing(true);
      else setError(e);
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

  if (loading) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje članarine" />
      </View>
    );
  }

  if (error && !membership) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={error} onRetry={() => { setLoading(true); load(); }} />
      </View>
    );
  }

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={styles.content}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={refresh} tintColor={colors.primary}
          colors={[colors.primary]} progressBackgroundColor={colors.surface} />
      }
    >
      {missing ? (
        <EmptyState icon="card-outline" title="Nemate članarinu"
          message="Članarinu možete uplatiti na recepciji teretane." />
      ) : (
        <>
          <Text style={[styles.status, membership.isActive ? styles.active : styles.inactive]}
            accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
            {membership.isActive ? 'Aktivna članarina' : 'Članarina je istekla'}
          </Text>
          <Text style={styles.validUntil} maxFontSizeMultiplier={MAX_FONT_SCALE}>
            Važi do {formatDate(membership.validUntil)}
          </Text>

          <View style={styles.qrBox}>
            <Image source={{ uri: membership.qrCodeUrl }} style={styles.qr} resizeMode="contain"
              accessibilityLabel="QR kod za ulazak u teretanu" />
          </View>
          <Text style={styles.hint} maxFontSizeMultiplier={MAX_FONT_SCALE}>
            {membership.isActive
              ? 'Pokažite ovaj kod na recepciji pri ulasku i izlasku.'
              : 'Ulazak nije dozvoljen dok ne produžite članarinu na recepciji.'}
          </Text>

          <View style={styles.card}>
            <Text style={styles.rowLabel} maxFontSizeMultiplier={MAX_FONT_SCALE}>Datum uplate</Text>
            <Text style={styles.rowValue} maxFontSizeMultiplier={MAX_FONT_SCALE}>{formatDate(membership.paymentDate)}</Text>
            <Text style={styles.rowLabel} maxFontSizeMultiplier={MAX_FONT_SCALE}>Cijena</Text>
            <Text style={styles.rowValue} maxFontSizeMultiplier={MAX_FONT_SCALE}>{Number(membership.price).toFixed(2)} KM</Text>
          </View>
        </>
      )}
      {error ? <ErrorMessage error={error} inline /> : null}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  centered: { alignItems: 'center', justifyContent: 'center' },
  content: { flexGrow: 1, padding: spacing.px24, gap: spacing.px16, alignItems: 'center' },
  status: { ...typography.screenTitle, textAlign: 'center' },
  active: { color: colors.primary },
  inactive: { color: colors.error },
  validUntil: { ...typography.body },
  qrBox: { padding: spacing.px12, borderRadius: borderRadius.px10, backgroundColor: '#FFFFFF' },
  qr: { width: QR_SIZE, height: QR_SIZE },
  hint: { ...typography.body, textAlign: 'center' },
  card: { alignSelf: 'stretch', gap: spacing.px4, padding: spacing.px16, borderRadius: borderRadius.px10, backgroundColor: colors.surface },
  rowLabel: { ...typography.caption },
  rowValue: { ...typography.body },
});