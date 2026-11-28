import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  FlatList,
  Pressable,
  RefreshControl,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { EmptyState, ErrorMessage } from '../../components';
import { theme } from '../../theme';
import {
  getNotifications,
  markNotificationAsRead,
  markAllNotificationsAsRead,
} from '../../services/notificationsService';
import { useAuth } from '../../context/AuthContext';
import {
  FollowRequestsSection,
  useFollowRequests,
  hasFollowRequestsContent,
} from '../trainer/FollowRequestsSection';

const { colors, fonts, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;
const DOT_SIZE = 10;

const pad = (n) => String(n).padStart(2, '0');

const parseDate = (value) => {
  if (!value) return null;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d;
};

const formatDateTime = (value) => {
  const d = parseDate(value);
  if (!d) return value ? String(value) : '';
  return `${pad(d.getDate())}.${pad(d.getMonth() + 1)}.${d.getFullYear()}. u ${pad(d.getHours())}:${pad(d.getMinutes())}`;
};

const sortNewestFirst = (list) =>
  [...list].sort((a, b) => {
    const ta = parseDate(a.date)?.getTime() ?? -Infinity;
    const tb = parseDate(b.date)?.getTime() ?? -Infinity;
    return tb - ta;
  });

const NotificationItem = ({ item, onPress }) => {
  const unread = item.isSeen === false;
  const when = formatDateTime(item.date);

  return (
    <Pressable
      onPress={unread ? () => onPress(item) : undefined}
      disabled={!unread}
      style={({ pressed }) => [
        styles.item,
        unread ? styles.itemUnread : styles.itemRead,
        pressed && styles.pressed,
      ]}
      accessible
      accessibilityRole={unread ? 'button' : undefined}
      accessibilityHint={unread ? 'Označava obavještenje kao pročitano' : undefined}
      accessibilityLabel={`${unread ? 'Nepročitano. ' : ''}${item.content || ''}. ${when}`}
    >
      <View style={styles.itemRow}>
        <Text
          style={[styles.message, unread ? styles.contentUnread : styles.contentRead]}
          maxFontSizeMultiplier={MAX_FONT_SCALE}
        >
          {item.content}
        </Text>
        {unread ? <View style={styles.dot} /> : null}
      </View>

      <View style={styles.dateRow}>
        <Ionicons name="time-outline" size={14} color={colors.textMuted} />
        <Text style={styles.date} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {when}
        </Text>
      </View>
    </Pressable>
  );
};

const MarkAllReadButton = ({ onPress }) => (
  <Pressable
    onPress={onPress}
    hitSlop={12}
    style={({ pressed }) => [styles.markAll, pressed && styles.pressed]}
    accessibilityRole="button"
    accessibilityLabel="Označi sve kao pročitano"
  >
    <Ionicons name="checkmark-done-outline" size={18} color={colors.primary} />
    <Text style={styles.markAllText} maxFontSizeMultiplier={MAX_FONT_SCALE}>
      Označi sve kao pročitano
    </Text>
  </Pressable>
);

const setSeen = (list, predicate) =>
  list.map((n) => (predicate(n) ? { ...n, isSeen: true } : n));

export default function NotificationsScreen() {
  const [notifications, setNotifications] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [loadError, setLoadError] = useState(null);

  // Trener na vrhu vidi i zahtjeve za praćenje (SU-32, SU-33). Za ostale uloge hook ne radi ništa.
  const { role } = useAuth();
  const isTrainer = role === 'TRAINER';
  const followRequests = useFollowRequests(isTrainer);
  const { reload: reloadFollowRequests, refresh: refreshFollowRequests } = followRequests;

  const loadNotifications = useCallback(async () => {
    setLoadError(null);
    try {
      setNotifications(sortNewestFirst(await getNotifications()));
    } catch (e) {
      setLoadError(e);
    }
  }, []);

  // Obavještenja i zahtjevi se učitavaju nezavisno: greška jedne rute ne sakriva drugu sekciju
  useEffect(() => {
    Promise.allSettled([loadNotifications(), reloadFollowRequests()]).finally(() => setLoading(false));
  }, [loadNotifications, reloadFollowRequests]);

  const retry = async () => {
    setLoading(true);
    await loadNotifications();
    setLoading(false);
  };

  const refresh = async () => {
    setRefreshing(true);
    await Promise.allSettled([loadNotifications(), refreshFollowRequests()]);
    setRefreshing(false);
  };

  // Optimistično ažuriranje: stavka odmah postaje pročitana; ako zahtjev padne, vraća se na nepročitano.
  const markOneAsRead = async (item) => {
    const id = item.notificationId;
    setNotifications((prev) => setSeen(prev, (n) => n.notificationId === id));
    try {
      await markNotificationAsRead(id);
    } catch {
      setNotifications((prev) =>
        prev.map((n) => (n.notificationId === id ? { ...n, isSeen: false } : n))
      );
    }
  };

  const markAllAsRead = async () => {
    const unreadIds = new Set(
      notifications.filter((n) => n.isSeen === false).map((n) => n.notificationId)
    );
    setNotifications((prev) => setSeen(prev, () => true));
    try {
      await markAllNotificationsAsRead();
    } catch {
      setNotifications((prev) =>
        prev.map((n) => (unreadIds.has(n.notificationId) ? { ...n, isSeen: false } : n))
      );
    }
  };

  if (loading && !notifications) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje obavještenja" />
      </View>
    );
  }

  // Trener i bez obavještenja vidi zahtjeve; greška obavještenja je tada u listi ispod njih
  if (!notifications && !isTrainer) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={loadError} onRetry={retry} />
      </View>
    );
  }

  const notificationsFailed = !notifications;
  const hasUnread = (notifications ?? []).some((n) => n.isSeen === false);

  const standardHeader =
    (loadError && !notificationsFailed) || hasUnread ? (
      <View style={styles.header}>
        {loadError && !notificationsFailed ? <ErrorMessage error={loadError} inline style={styles.inlineError} /> : null}
        {hasUnread ? <MarkAllReadButton onPress={markAllAsRead} /> : null}
      </View>
    ) : null;

  const showFollowRequests = isTrainer && hasFollowRequestsContent(followRequests);

  return (
    <View style={styles.screen}>
      <FlatList
        data={notifications ?? []}
        keyExtractor={(item, index) => String(item.notificationId ?? index)}
        renderItem={({ item }) => <NotificationItem item={item} onPress={markOneAsRead} />}
        contentContainerStyle={styles.content}
        ListHeaderComponent={
          showFollowRequests ? (
            <View style={styles.header}>
              <FollowRequestsSection state={followRequests} />
              <Text style={styles.sectionTitle} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
                Obavještenja
              </Text>
              {standardHeader}
            </View>
          ) : (
            standardHeader
          )
        }
        ListEmptyComponent={
          notificationsFailed ? (
            <ErrorMessage error={loadError} onRetry={retry} />
          ) : (
            <EmptyState
              icon="notifications-off-outline"
              title="Nema obavještenja"
              message="Ovdje će se pojaviti nova obavještenja."
            />
          )
        }
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
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  centered: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  content: {
    flexGrow: 1,
    padding: spacing.px16,
    gap: spacing.px12,
  },
  inlineError: {
    marginBottom: spacing.px4,
  },
  header: {
    gap: spacing.px8,
  },
  sectionTitle: {
    ...typography.sectionTitle,
    marginTop: spacing.px12,
  },
  markAll: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-end',
    gap: spacing.px4,
  },
  markAllText: {
    ...typography.caption,
    fontFamily: fonts.inter500,
    color: colors.primary,
  },
  pressed: {
    opacity: 0.7,
  },
  item: {
    gap: spacing.px8,
    padding: spacing.px16,
    borderRadius: borderRadius.px10,
    borderLeftWidth: 3,
  },
  itemUnread: {
    backgroundColor: colors.surface,
    borderLeftColor: colors.primary,
  },
  itemRead: {
    backgroundColor: colors.background,
    borderLeftColor: colors.surface,
    borderWidth: 1,
    borderColor: colors.surface,
  },
  itemRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: spacing.px12,
  },
  message: {
    ...typography.body,
    flex: 1,
  },
  contentUnread: {
    fontFamily: fonts.inter600,
    color: colors.textPrimary,
  },
  contentRead: {
    color: colors.textSecondary,
  },
  dot: {
    width: DOT_SIZE,
    height: DOT_SIZE,
    borderRadius: DOT_SIZE / 2,
    marginTop: spacing.px6,
    backgroundColor: colors.primary,
  },
  dateRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.px4,
  },
  date: {
    ...typography.caption,
  },
});
