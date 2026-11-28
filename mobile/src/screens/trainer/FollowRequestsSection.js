import React, { useCallback, useState } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Button, ErrorMessage } from '../../components';
import { theme } from '../../theme';
import {
  getFollowRequests,
  respondToFollowRequest,
  FOLLOW_REQUEST_STATUS,
} from '../../services/trainerService';
import { formatDate } from '../../utils/date';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;

const FALLBACK_MESSAGES = {
  ACCEPTED: 'Zahtjev je prihvaćen.',
  REJECTED: 'Zahtjev je odbijen.',
};
const GONE_MESSAGE = 'Zahtjev više nije dostupan. Lista je osvježena.';

// Stanje i akcije za zahtjeve za praćenje trenera (SU-32, SU-33).
// enabled = false (sve uloge osim TRAINER): ništa se ne učitava, reload/refresh odmah završavaju.
export function useFollowRequests(enabled) {
  const [requests, setRequests] = useState(null);
  const [loadError, setLoadError] = useState(null);
  // { requestId, status } zahtjeva koji se upravo šalje; dok traje, ostala dugmad su ugašena
  const [pending, setPending] = useState(null);
  const [actionError, setActionError] = useState(null);
  const [notice, setNotice] = useState(null);

  const reload = useCallback(async () => {
    if (!enabled) return;
    try {
      setRequests(await getFollowRequests());
      setLoadError(null);
    } catch (e) {
      setLoadError(e);
    }
  }, [enabled]);

  // Povlačenje nadolje: brišu se stare poruke pa se lista ponovo učita
  const refresh = useCallback(async () => {
    setNotice(null);
    setActionError(null);
    await reload();
  }, [reload]);

  const respond = useCallback(async (request, status) => {
    setPending({ requestId: request.requestId, status });
    setActionError(null);
    setNotice(null);
    try {
      const result = await respondToFollowRequest(request.requestId, status);
      setRequests((list) => (list ?? []).filter((r) => r.requestId !== request.requestId));
      setNotice(result?.message || FALLBACK_MESSAGES[status]);
    } catch (e) {
      const code = e.response?.status;
      if (code === 404 || code === 409) {
        // 404: član je povukao zahtjev ili je obrisan; 409: zahtjev je već obrađen
        setNotice(e.response?.data?.message || GONE_MESSAGE);
        await reload();
      } else {
        setActionError(e);
      }
    } finally {
      setPending(null);
    }
  }, [reload]);

  return { requests, loadError, pending, actionError, notice, reload, refresh, respond };
}

const RequestCard = ({ request, busyStatus, disabled, onRespond }) => (
  <View style={styles.card}>
    <View style={styles.cardHeader}>
      <Ionicons name="person-circle-outline" size={40} color={colors.iconMuted} />
      <View style={styles.cardTexts}>
        <Text style={styles.memberName} numberOfLines={1} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {request.memberName}
        </Text>
        <Text style={styles.requestDate} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          Zahtjev poslan {formatDate(request.requestDate)}
        </Text>
      </View>
    </View>
    <View style={styles.actions}>
      <Button
        title="Prihvati"
        variant="confirm"
        onPress={() => onRespond(request, FOLLOW_REQUEST_STATUS.ACCEPTED)}
        loading={busyStatus === FOLLOW_REQUEST_STATUS.ACCEPTED}
        disabled={disabled}
        style={styles.actionButton}
      />
      <Button
        title="Odbij"
        variant="danger"
        onPress={() => onRespond(request, FOLLOW_REQUEST_STATUS.REJECTED)}
        loading={busyStatus === FOLLOW_REQUEST_STATUS.REJECTED}
        disabled={disabled}
        style={styles.actionButton}
      />
    </View>
  </View>
);

// Da li sekcija ima šta da prikaže. Bez zahtjeva, poruka i grešaka sekcija se sakriva.
export const hasFollowRequestsContent = ({ requests, loadError, actionError, notice }) =>
  (requests?.length ?? 0) > 0 || !!loadError || !!actionError || !!notice;

// Sekcija "Zahtjevi za praćenje" na vrhu ekrana obavještenja (samo TRAINER).
// state: rezultat useFollowRequests()
export const FollowRequestsSection = ({ state, style }) => {
  const { requests, loadError, pending, actionError, notice, respond } = state;
  if (!hasFollowRequestsContent(state)) return null;

  const list = requests ?? [];

  return (
    <View style={[styles.section, style]}>
      <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
        Zahtjevi za praćenje{list.length ? ` (${list.length})` : ''}
      </Text>

      {notice ? (
        <View style={styles.notice} accessibilityRole="alert" accessibilityLiveRegion="polite">
          <Ionicons name="checkmark-circle-outline" size={18} color={colors.primary} />
          <Text style={styles.noticeText} maxFontSizeMultiplier={MAX_FONT_SCALE}>{notice}</Text>
        </View>
      ) : null}

      <ErrorMessage error={actionError || loadError} inline />

      {list.length === 0 && requests ? (
        <Text style={styles.empty} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          Nema zahtjeva za praćenje na čekanju.
        </Text>
      ) : null}

      {list.map((request) => (
        <RequestCard
          key={request.requestId}
          request={request}
          busyStatus={pending?.requestId === request.requestId ? pending.status : null}
          disabled={!!pending}
          onRespond={respond}
        />
      ))}
    </View>
  );
};

const styles = StyleSheet.create({
  section: { gap: spacing.px12 },
  title: { ...typography.sectionTitle },
  notice: { flexDirection: 'row', alignItems: 'center', gap: spacing.px6 },
  noticeText: { ...typography.body, color: colors.primary, flex: 1 },
  empty: { ...typography.body, color: colors.textSecondary },
  card: { gap: spacing.px12, padding: spacing.px16, borderRadius: borderRadius.px10, backgroundColor: colors.surface },
  cardHeader: { flexDirection: 'row', alignItems: 'center', gap: spacing.px12 },
  cardTexts: { flex: 1, gap: spacing.px2 },
  memberName: { ...typography.label },
  requestDate: { ...typography.caption, color: colors.textSecondary },
  actions: { flexDirection: 'row', gap: spacing.px12 },
  actionButton: { flex: 1 },
});
