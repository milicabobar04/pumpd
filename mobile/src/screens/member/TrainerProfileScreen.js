import React, { useCallback, useState } from 'react';
import { View, Text, ScrollView, RefreshControl, ActivityIndicator, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import { Button, ConfirmDialog, EmptyState, ErrorMessage, StarRating } from '../../components';
import { theme, cards } from '../../theme';
import { getTrainer, followTrainer, unfollowTrainer } from '../../services/trainersService';
import { formatDate } from '../../utils/date';
import { formatGrade } from '../../utils/grade';

const { colors, spacing, borderRadius, typography } = theme;
const MAX_FONT_SCALE = 1.3;

const FOLLOW_BUTTON = {
  NONE: { title: 'Prati', variant: 'confirm' },
  PENDING: { title: 'Čeka', variant: 'secondary' },
  ACCEPTED: { title: 'Prati se', variant: 'secondary' },
};

const UNFOLLOW_DIALOG = {
  PENDING: {
    title: 'Povlačenje zahtjeva',
    message: (name) => `Da li želite povući zahtjev za praćenje trenera ${name}?`,
    confirmLabel: 'Povuci',
  },
  ACCEPTED: {
    title: 'Prekid praćenja',
    message: (name) => `Da li želite prestati pratiti trenera ${name}? Više nećete vidjeti njegove objave.`,
    confirmLabel: 'Prekini',
  },
};

const ReviewItem = ({ review }) => (
  <View style={styles.review}>
    <View style={styles.reviewHeader}>
      <Text style={[cards.review.title, styles.reviewName]} numberOfLines={1} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        {review.memberName}
      </Text>
      <Text style={cards.review.body} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        {formatDate(review.date)}
      </Text>
    </View>
    <StarRating value={review.grade} size={18} color={colors.background} emptyColor={colors.gray500} />
    {review.comment?.trim() ? (
      <Text style={cards.review.body} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        {review.comment}
      </Text>
    ) : null}
  </View>
);

export default function TrainerProfileScreen({ navigation, route }) {
  const { trainerId, reviewMessage } = route.params;
  const [trainer, setTrainer] = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [loadError, setLoadError] = useState(null);
  const [notFound, setNotFound] = useState(false);

  const [following, setFollowing] = useState(false);
  const [followError, setFollowError] = useState(null);
  const [confirmOpen, setConfirmOpen] = useState(false);

  const load = useCallback(async () => {
    setLoadError(null);
    try {
      setTrainer(await getTrainer(trainerId));
      setNotFound(false);
    } catch (e) {
      if (e.response?.status === 404) setNotFound(true);
      else setLoadError(e);
    } finally {
      setLoading(false);
    }
  }, [trainerId]);

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

  const setFollowStatus = (followStatus) => setTrainer((t) => (t ? { ...t, followStatus } : t));

  const follow = async () => {
    setFollowing(true);
    setFollowError(null);
    try {
      await followTrainer(trainerId);
      setFollowStatus('PENDING');
    } catch (e) {
      if (e.response?.status === 409) await load();
      else if (e.response?.status === 404) setNotFound(true);
      else setFollowError(e);
    } finally {
      setFollowing(false);
    }
  };

  const unfollow = async () => {
    setFollowing(true);
    setFollowError(null);
    try {
      await unfollowTrainer(trainerId);
      setFollowStatus('NONE');
      setConfirmOpen(false);
    } catch (e) {
      setConfirmOpen(false);
      if (e.response?.status === 404) await load();
      else setFollowError(e);
    } finally {
      setFollowing(false);
    }
  };

  const onFollowPress = () => {
    if (trainer.followStatus === 'PENDING' || trainer.followStatus === 'ACCEPTED') {
      setFollowError(null);
      setConfirmOpen(true);
    } else {
      follow();
    }
  };

  if (loading && !trainer && !notFound) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje trenera" />
      </View>
    );
  }

  if (notFound) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <EmptyState
          icon="person-remove-outline"
          title="Trener nije pronađen"
          message="Ovaj trener više nije dostupan."
        />
        <Button title="Nazad na listu" variant="action" onPress={() => navigation.goBack()} />
      </View>
    );
  }

  if (!trainer) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={loadError} onRetry={() => { setLoading(true); load(); }} />
      </View>
    );
  }

  const fullName = `${trainer.name} ${trainer.surname}`;
  const status = FOLLOW_BUTTON[trainer.followStatus] ? trainer.followStatus : 'NONE';
  const followButton = FOLLOW_BUTTON[status];
  const dialog = UNFOLLOW_DIALOG[status] ?? UNFOLLOW_DIALOG.ACCEPTED;

  return (
    <View style={styles.screen}>
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
        <View style={styles.headerRow}>
          <Ionicons name="person-circle-outline" size={64} color={colors.iconMuted} />
          <View style={styles.headerTexts}>
            <Text style={styles.title} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
              {fullName}
            </Text>
            <View style={styles.gradeRow}>
              {trainer.averageGrade == null ? null : (
                <StarRating value={trainer.averageGrade} size={18} />
              )}
              <Text
                style={trainer.averageGrade == null ? styles.gradeEmpty : styles.gradeText}
                maxFontSizeMultiplier={MAX_FONT_SCALE}
                accessibilityLabel={`Prosječna ocjena ${formatGrade(trainer.averageGrade)}`}
              >
                {formatGrade(trainer.averageGrade)}
              </Text>
            </View>
          </View>
        </View>

        <Button
          title={followButton.title}
          variant={followButton.variant}
          onPress={onFollowPress}
          loading={following && !confirmOpen}
          disabled={following}
          style={styles.followButton}
        />
        <ErrorMessage error={followError} inline />

        <View style={styles.card}>
          <Text style={styles.rowLabel} maxFontSizeMultiplier={MAX_FONT_SCALE}>Biografija</Text>
          <Text style={styles.rowValue} maxFontSizeMultiplier={MAX_FONT_SCALE}>
            {trainer.biography?.trim() ? trainer.biography : 'Trener još nije dodao biografiju.'}
          </Text>
        </View>

        <View style={styles.sectionHeader}>
          <Text style={styles.sectionTitle} accessibilityRole="header" maxFontSizeMultiplier={MAX_FONT_SCALE}>
            Recenzije ({trainer.reviews.length})
          </Text>
        </View>

        {reviewMessage ? (
          <View style={styles.notice} accessibilityRole="alert" accessibilityLiveRegion="polite">
            <Ionicons name="checkmark-circle-outline" size={18} color={colors.primary} />
            <Text style={styles.noticeText} maxFontSizeMultiplier={MAX_FONT_SCALE}>{reviewMessage}</Text>
          </View>
        ) : null}

        <Button
          title="Ostavi recenziju"
          variant="action"
          onPress={() => {
            navigation.setParams({ reviewMessage: undefined });
            navigation.navigate('MemberTrainerReview', { trainerId, trainerName: fullName });
          }}
          style={styles.reviewButton}
        />

        {trainer.reviews.length === 0 ? (
          <EmptyState
            icon="chatbubble-ellipses-outline"
            title="Nema recenzija"
            message="Budite prvi koji će ocijeniti ovog trenera."
            style={styles.emptyReviews}
          />
        ) : (
          trainer.reviews.map((review) => <ReviewItem key={review.reviewId} review={review} />)
        )}
      </ScrollView>

      <ConfirmDialog
        visible={confirmOpen}
        title={dialog.title}
        message={dialog.message(fullName)}
        confirmLabel={dialog.confirmLabel}
        confirmVariant="danger"
        loading={following}
        onConfirm={unfollow}
        onCancel={() => setConfirmOpen(false)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  centered: { alignItems: 'center', justifyContent: 'center' },
  content: { flexGrow: 1, padding: spacing.px24, gap: spacing.px16 },
  headerRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.px12 },
  headerTexts: { flex: 1, gap: spacing.px4 },
  title: { ...typography.screenTitle, color: colors.primary },
  gradeRow: { flexDirection: 'row', alignItems: 'center', gap: spacing.px8 },
  gradeText: { ...typography.body, color: colors.primary },
  gradeEmpty: { ...typography.body, color: colors.textSecondary },
  followButton: { alignSelf: 'flex-start', minWidth: 120 },
  card: { gap: spacing.px4, padding: spacing.px16, borderRadius: borderRadius.px10, backgroundColor: colors.surface },
  rowLabel: { ...typography.caption, color: colors.textSecondary },
  rowValue: { ...typography.body },
  sectionHeader: { marginTop: spacing.px8 },
  sectionTitle: { ...typography.sectionTitle },
  notice: { flexDirection: 'row', alignItems: 'center', gap: spacing.px6 },
  noticeText: { ...typography.body, color: colors.primary, flex: 1 },
  reviewButton: { alignSelf: 'center' },
  emptyReviews: { flexGrow: 0, paddingVertical: spacing.px16 },
  review: { ...cards.review.container, gap: spacing.px6 },
  reviewHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: spacing.px8 },
  reviewName: { flex: 1 },
});
