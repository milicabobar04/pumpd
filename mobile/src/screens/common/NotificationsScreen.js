import React, { useCallback, useEffect, useState } from 'react';
import {
  View,
  Text,
  FlatList,
  RefreshControl,
  ActivityIndicator,
  StyleSheet,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { EmptyState, ErrorMessage } from '../../components';
import { theme } from '../../theme';
import { getNotifications } from '../../services/notificationsService';

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

const NotificationItem = ({ item }) => {
  const unread = item.isSeen === false;
  const when = formatDateTime(item.date);

  return (
    <View
      style={[styles.item, unread ? styles.itemUnread : styles.itemRead]}
      accessible
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
    </View>
  );
};

export default function NotificationsScreen() {
  const [notifications, setNotifications] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [loadError, setLoadError] = useState(null);

  const loadNotifications = useCallback(async () => {
    setLoadError(null);
    try {
      setNotifications(sortNewestFirst(await getNotifications()));
    } catch (e) {
      setLoadError(e);
    }
  }, []);

  useEffect(() => {
    loadNotifications().finally(() => setLoading(false));
  }, [loadNotifications]);

  const retry = async () => {
    setLoading(true);
    await loadNotifications();
    setLoading(false);
  };

  const refresh = async () => {
    setRefreshing(true);
    await loadNotifications();
    setRefreshing(false);
  };

  if (loading && !notifications) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje obavještenja" />
      </View>
    );
  }

  if (!notifications) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={loadError} onRetry={retry} />
      </View>
    );
  }

  return (
    <View style={styles.screen}>
      <FlatList
        data={notifications}
        keyExtractor={(item, index) => String(item.notificationId ?? index)}
        renderItem={({ item }) => <NotificationItem item={item} />}
        contentContainerStyle={styles.content}
        ListHeaderComponent={
          loadError ? <ErrorMessage error={loadError} inline style={styles.inlineError} /> : null
        }
        ListEmptyComponent={
          <EmptyState
            icon="notifications-off-outline"
            title="Nema obavještenja"
            message="Ovdje će se pojaviti nova obavještenja."
          />
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
